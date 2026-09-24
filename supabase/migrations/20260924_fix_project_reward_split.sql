-- TaskEarn: canonical project reward split and project credit path
-- Applied to Supabase project wfvmipwesttejtvwoasj on 2026-09-24.
-- User share: <= ₹500 => 45%; > ₹500 => 50%.

CREATE OR REPLACE FUNCTION public.set_project_user_reward()
RETURNS trigger
LANGUAGE plpgsql
SET search_path TO 'public'
AS $function$
BEGIN
  NEW.student_reward := public.calculate_project_user_reward(COALESCE(NEW.company_payout,0));
  RETURN NEW;
END;
$function$;

DROP TRIGGER IF EXISTS trg_set_project_user_reward ON public.projects;
CREATE TRIGGER trg_set_project_user_reward
BEFORE INSERT OR UPDATE OF company_payout ON public.projects
FOR EACH ROW
EXECUTE FUNCTION public.set_project_user_reward();

CREATE OR REPLACE FUNCTION public.credit_project_reward()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $function$
DECLARE
  company_amount numeric := 0;
  user_reward numeric := 0;
BEGIN
  IF NEW.status <> 'approved'
     OR COALESCE(NEW.reward_credited,false) = true
     OR (TG_OP = 'UPDATE' AND OLD.status IS NOT DISTINCT FROM 'approved')
  THEN
    RETURN NEW;
  END IF;

  SELECT COALESCE(company_payout,0)
    INTO company_amount
  FROM public.projects
  WHERE id = NEW.project_id
  FOR UPDATE;

  IF company_amount <= 0 THEN
    NEW.reward := 0;
    NEW.reward_credited := true;
    NEW.reviewed_at := COALESCE(NEW.reviewed_at, now());
    RETURN NEW;
  END IF;

  user_reward := public.calculate_project_user_reward(company_amount);
  NEW.reward := user_reward;

  IF user_reward > 0 THEN
    INSERT INTO public.wallet_transactions
      (user_id, submission_id, amount, type, created_at)
    VALUES
      (NEW.user_id, NULL, user_reward, 'project_reward', now());
  END IF;

  NEW.reward_credited := true;
  NEW.reviewed_at := COALESCE(NEW.reviewed_at, now());
  RETURN NEW;
END;
$function$;
