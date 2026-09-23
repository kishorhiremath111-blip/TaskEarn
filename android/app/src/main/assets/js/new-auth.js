// ======================================================
// TASKEARN — NEW AUTH SYSTEM
// LOGIN + REGISTER
// SUPABASE AUTH
// ======================================================

(function () {

    "use strict";


    // ==================================================
    // SUPABASE CLIENT
    // ==================================================

    function getClient() {

        if (
            typeof supabaseClient === "undefined" ||
            !supabaseClient
        ) {
            alert(
                "Supabase is not connected."
            );

            return null;
        }

        return supabaseClient;
    }


    // ==================================================
    // REMOVE OLD AUTH MODALS
    // ==================================================

    function removeAuth() {

        const ids = [
            "authModal",
            "newAuthModal",
            "taskAuthModal"
        ];

        ids.forEach(function (id) {

            const element =
                document.getElementById(id);

            if (element) {
                element.remove();
            }

        });
    }


    // ==================================================
    // AUTH STYLES
    // ==================================================

    function addStyles() {

        if (
            document.getElementById(
                "newTaskEarnAuthCSS"
            )
        ) {
            return;
        }

        const style =
            document.createElement("style");

        style.id =
            "newTaskEarnAuthCSS";

        style.textContent = `

        .te-auth-backdrop {
            position: fixed;
            inset: 0;
            z-index: 999999;

            display: flex;
            align-items: center;
            justify-content: center;

            padding: 18px;

            background:
                radial-gradient(
                    circle at top left,
                    rgba(0,190,255,.18),
                    transparent 35%
                ),
                radial-gradient(
                    circle at bottom right,
                    rgba(130,70,255,.20),
                    transparent 40%
                ),
                rgba(3,7,20,.94);

            backdrop-filter: blur(14px);
            -webkit-backdrop-filter: blur(14px);

            overflow-y: auto;
        }


        .te-auth-box {
            width: min(430px, 100%);
            max-height: 94vh;

            overflow-y: auto;

            position: relative;

            padding: 28px 22px 24px;

            border:
                1px solid
                rgba(255,255,255,.12);

            border-radius: 28px;

            background:
                linear-gradient(
                    145deg,
                    #111a38,
                    #0b1026 55%,
                    #11102d
                );

            box-shadow:
                0 25px 80px
                rgba(0,0,0,.55),

                0 0 45px
                rgba(40,150,255,.08);

            color: white;
        }


        .te-auth-close {
            position: absolute;

            right: 16px;
            top: 16px;

            width: 42px;
            height: 42px;

            border: 0;
            border-radius: 50%;

            background:
                rgba(255,255,255,.08);

            color: white;

            font-size: 25px;

            cursor: pointer;
        }


        .te-auth-logo {
            width: 72px;
            height: 72px;

            margin: 4px auto 16px;

            display: flex;
            align-items: center;
            justify-content: center;

            border-radius: 20px;

            background:
                linear-gradient(
                    135deg,
                    #12b9ff,
                    #6948ff
                );

            box-shadow:
                0 12px 35px
                rgba(45,130,255,.30);

            font-size: 32px;
        }


        .te-auth-tag {
            display: block;

            width: fit-content;

            margin: 0 auto 10px;

            padding: 7px 14px;

            border-radius: 999px;

            background:
                rgba(35,210,255,.08);

            border:
                1px solid
                rgba(35,210,255,.28);

            color: #62e6ff;

            font-size: 11px;
            font-weight: 800;

            letter-spacing: 1px;
        }


        .te-auth-title {
            margin: 0;

            text-align: center;

            font-size: 30px;
            font-weight: 800;
        }


        .te-auth-subtitle {
            margin: 10px auto 22px;

            max-width: 330px;

            text-align: center;

            color: #aeb7d2;

            line-height: 1.5;

            font-size: 14px;
        }


        .te-auth-label {
            display: block;

            margin:
                14px 2px 7px;

            color: #dce4ff;

            font-size: 13px;

            font-weight: 700;
        }


        .te-auth-input-wrap {
            position: relative;
        }


        .te-auth-input {
            width: 100%;
            height: 52px;

            box-sizing: border-box;

            padding: 0 15px;

            border-radius: 15px;

            border:
                1px solid
                rgba(255,255,255,.13);

            outline: none;

            background: #0d142d;

            color: white;

            font-size: 15px;
        }


        .te-auth-input::placeholder {
            color: #707b9b;
        }


        .te-auth-input:focus {
            border-color: #35bfff;

            box-shadow:
                0 0 0 3px
                rgba(53,191,255,.10);
        }


        .te-auth-password {
            padding-right: 55px;
        }


        .te-auth-eye {
            position: absolute;

            right: 8px;
            top: 7px;

            width: 38px;
            height: 38px;

            border: 0;

            border-radius: 11px;

            background:
                rgba(255,255,255,.07);

            color: white;

            cursor: pointer;

            font-size: 17px;
        }


        .te-auth-main-btn {
            width: 100%;
            height: 54px;

            margin-top: 22px;

            border: 0;

            border-radius: 16px;

            background:
                linear-gradient(
                    100deg,
                    #10baff,
                    #6848ff
                );

            color: white;

            font-size: 16px;

            font-weight: 800;

            cursor: pointer;

            box-shadow:
                0 12px 30px
                rgba(70,100,255,.22);
        }


        .te-auth-main-btn:disabled {
            opacity: .6;
            cursor: not-allowed;
        }


        .te-auth-switch {
            width: 100%;
            min-height: 50px;

            margin-top: 12px;

            border-radius: 15px;

            border:
                1px solid
                rgba(255,255,255,.12);

            background:
                rgba(255,255,255,.04);

            color: #dbe3ff;

            font-weight: 700;

            cursor: pointer;
        }


        .te-auth-note {
            margin-top: 15px;

            text-align: center;

            color: #747f9f;

            font-size: 11px;
        }


        @media (max-width: 500px) {

            .te-auth-backdrop {
                padding: 12px;
            }

            .te-auth-box {
                padding:
                    24px 18px 22px;

                border-radius: 24px;
            }

            .te-auth-title {
                font-size: 26px;
            }

        }

        `;

        document.head.appendChild(style);
    }


    // ==================================================
    // CREATE MODAL
    // ==================================================

    function createModal() {

        removeAuth();

        addStyles();

        const modal =
            document.createElement("div");

        modal.id =
            "taskAuthModal";

        modal.className =
            "te-auth-backdrop";

        document.body.appendChild(modal);

        return modal;
    }


    // ==================================================
    // PASSWORD TOGGLE
    // ==================================================

    window.teTogglePassword =
        function (id, button) {

            const input =
                document.getElementById(id);

            if (!input) {
                return;
            }

            if (
                input.type === "password"
            ) {

                input.type = "text";

                button.textContent = "🙈";

            } else {

                input.type = "password";

                button.textContent = "👁️";
            }

        };


    // ==================================================
    // LOGIN MODAL
    // ==================================================

    window.openLogin = function () {

        const modal =
            createModal();

        modal.innerHTML = `

            <div class="te-auth-box">

                <button
                    class="te-auth-close"
                    type="button"
                    onclick="closeAuthModal()"
                >
                    ×
                </button>


                <div class="te-auth-logo">
                    🔐
                </div>


                <span class="te-auth-tag">
                    TASKEARN ACCOUNT
                </span>


                <h2 class="te-auth-title">
                    Welcome Back
                </h2>


                <p class="te-auth-subtitle">
                    Login to continue to your
                    tasks, rewards and wallet.
                </p>


                <label class="te-auth-label">
                    Email Address
                </label>


                <input
                    id="teLoginEmail"
                    class="te-auth-input"
                    type="email"
                    placeholder="Enter your email"
                    autocomplete="email"
                >


                <label class="te-auth-label">
                    Password
                </label>


                <div
                    class="te-auth-input-wrap"
                >

                    <input
                        id="teLoginPassword"
                        class="
                            te-auth-input
                            te-auth-password
                        "
                        type="password"
                        placeholder="Enter your password"
                        autocomplete="current-password"
                    >


                    <button
                        type="button"
                        class="te-auth-eye"
                        onclick="
                            teTogglePassword(
                                'teLoginPassword',
                                this
                            )
                        "
                    >
                        👁️
                    </button>

                </div>


                <button
                    id="teLoginButton"
                    class="te-auth-main-btn"
                    type="button"
                    onclick="teLoginUser()"
                >
                    Login
                </button>


                <button
                    class="te-auth-switch"
                    type="button"
                    onclick="openRegister()"
                >
                    Create a new account
                </button>


                <div class="te-auth-note">
                    Your account is securely
                    handled by Supabase.
                </div>

            </div>

        `;
    };


    // ==================================================
    // REGISTER MODAL
    // ==================================================

    window.openRegister = function () {

        const modal =
            createModal();

        modal.innerHTML = `

            <div class="te-auth-box">

                <button
                    class="te-auth-close"
                    type="button"
                    onclick="closeAuthModal()"
                >
                    ×
                </button>


                <div class="te-auth-logo">
                    🚀
                </div>


                <span class="te-auth-tag">
                    JOIN TASKEARN
                </span>


                <h2 class="te-auth-title">
                    Create Account
                </h2>


                <p class="te-auth-subtitle">
                    Register once and start
                    using TaskEarn.
                </p>


                <label class="te-auth-label">
                    Full Name
                </label>


                <input
                    id="teRegName"
                    class="te-auth-input"
                    type="text"
                    placeholder="Enter your full name"
                    autocomplete="name"
                >


                <label class="te-auth-label">
                    Mobile Number
                </label>


                <input
                    id="teRegMobile"
                    class="te-auth-input"
                    type="tel"
                    placeholder="10-digit mobile number"
                    inputmode="numeric"
                    maxlength="10"
                    autocomplete="tel"
                >


                <label class="te-auth-label">
                    Email Address
                </label>


                <input
                    id="teRegEmail"
                    class="te-auth-input"
                    type="email"
                    placeholder="Enter your email"
                    autocomplete="email"
                >


                <label class="te-auth-label">
                    Password
                </label>


                <div
                    class="te-auth-input-wrap"
                >

                    <input
                        id="teRegPassword"
                        class="
                            te-auth-input
                            te-auth-password
                        "
                        type="password"
                        placeholder="Minimum 6 characters"
                        autocomplete="new-password"
                    >


                    <button
                        type="button"
                        class="te-auth-eye"
                        onclick="
                            teTogglePassword(
                                'teRegPassword',
                                this
                            )
                        "
                    >
                        👁️
                    </button>

                </div>


                <label class="te-auth-label">
                    Confirm Password
                </label>


                <div
                    class="te-auth-input-wrap"
                >

                    <input
                        id="teRegConfirm"
                        class="
                            te-auth-input
                            te-auth-password
                        "
                        type="password"
                        placeholder="Re-enter your password"
                        autocomplete="new-password"
                    >


                    <button
                        type="button"
                        class="te-auth-eye"
                        onclick="
                            teTogglePassword(
                                'teRegConfirm',
                                this
                            )
                        "
                    >
                        👁️
                    </button>

                </div>


                <label class="te-auth-label">

                    Referral Code

                    <span
                        style="color:#7180a5;"
                    >
                        (Optional)
                    </span>

                </label>


                <input
                    id="teRegReferral"
                    class="te-auth-input"
                    type="text"
                    placeholder="Enter referral code"
                    autocomplete="off"
                >


                <button
                    id="teRegisterButton"
                    class="te-auth-main-btn"
                    type="button"
                    onclick="teRegisterUser()"
                >
                    Create Account
                </button>


                <button
                    class="te-auth-switch"
                    type="button"
                    onclick="openLogin()"
                >
                    Already have an account?
                    Login
                </button>


                <div class="te-auth-note">
                    Keep your login details safe.
                </div>

            </div>

        `;
    };


    // ==================================================
    // CLOSE AUTH
    // ==================================================

    window.closeAuthModal = function () {

        removeAuth();
    };


    // ==================================================
    // LOGIN USER
    // ==================================================

    window.teLoginUser = async function () {

        const client =
            getClient();

        if (!client) {
            return;
        }


        const email =
            document
                .getElementById(
                    "teLoginEmail"
                )
                ?.value
                .trim() || "";


        const password =
            document
                .getElementById(
                    "teLoginPassword"
                )
                ?.value || "";


        if (!email) {

            alert(
                "Please enter your email address."
            );

            return;
        }


        if (!password) {

            alert(
                "Please enter your password."
            );

            return;
        }


        const button =
            document.getElementById(
                "teLoginButton"
            );


        if (button) {

            button.disabled = true;

            button.textContent =
                "Logging in...";
        }


        try {

            const result =
                await client.auth
                    .signInWithPassword({

                        email: email,

                        password: password

                    });


            if (result.error) {

                alert(
                    "Login failed:\n\n" +
                    result.error.message
                );

                return;
            }


            if (!result.data?.user) {

                alert(
                    "Login failed. Please try again."
                );

                return;
            }


            window.dispatchEvent(
                new CustomEvent(
                    "taskearn:login",
                    {
                        detail: {
                            user:
                                result.data.user
                        }
                    }
                )
            );


            closeAuthModal();


            setTimeout(
                function () {

                    window.location.reload();

                },
                250
            );


              } catch (error) {

            console.error(
                "Login error:",
                error
            );

            alert(
                error?.message ||
                "Something went wrong."
            );

        } finally {

            if (button) {

                button.disabled = false;

                button.textContent = "Login";

            }

        }

    };


    // ==================================================
    // REGISTER USER
    // ==================================================

    window.teRegisterUser = async function () {

        const client = getClient();

        if (!client) {
            return;
        }

        const name =
            document
                .getElementById("teRegName")
                ?.value
                .trim() || "";

        const mobile =
            document
                .getElementById("teRegMobile")
                ?.value
                .trim() || "";

        const email =
            document
                .getElementById("teRegEmail")
                ?.value
                .trim() || "";

        const password =
            document
                .getElementById("teRegPassword")
                ?.value || "";

        const confirmPassword =
            document
                .getElementById("teRegConfirm")
                ?.value || "";

        const referral =
            document
                .getElementById("teRegReferral")
                ?.value
                .trim() || "";


        if (!name) {
            alert("Please enter your full name.");
            return;
        }

        if (!/^[0-9]{10}$/.test(mobile)) {
            alert("Please enter a valid 10-digit mobile number.");
            return;
        }

        if (!email) {
            alert("Please enter your email address.");
            return;
        }

        if (password.length < 6) {
            alert("Password must be at least 6 characters.");
            return;
        }

        if (password !== confirmPassword) {
            alert("Passwords do not match.");
            return;
        }


        const button =
            document.getElementById(
                "teRegisterButton"
            );


        if (button) {

            button.disabled = true;

            button.textContent =
                "Creating Account...";

        }


        try {

            const result =
                await client.auth.signUp({

                    email: email,

                    password: password,

                    options: {

                        data: {

                            full_name: name,

                            mobile: mobile,

                            referral_code: referral

                        }

                    }

                });


            if (result.error) {

                alert(
                    "Signup failed:\n\n" +
                    result.error.message
                );

                return;
            }


            if (!result.data?.user) {

                alert(
                    "Account could not be created."
                );

                return;
            }


            alert(
                "Account created successfully! 🎉\n\n" +
                "You can now login."
            );


            openLogin();


        } catch (error) {

            console.error(
                "Register error:",
                error
            );

            alert(
                error?.message ||
                "Something went wrong."
            );

        } finally {

            if (button) {

                button.disabled = false;

                button.textContent =
                    "Create Account";

            }

        }

    };


    // ==================================================
    // SESSION CHECK
    // ==================================================

    window.taskEarnCheckSession =
        async function () {

            const client = getClient();

            if (!client) {
                return null;
            }

            try {

                const {
                    data,
                    error
                } =
                    await client.auth.getSession();


                if (error) {

                    console.error(
                        "Session error:",
                        error
                    );

                    return null;
                }


                return data?.session || null;


            } catch (error) {

                console.error(
                    "Session check error:",
                    error
                );

                return null;
            }

        };


    // ==================================================
    // AUTH STATE LISTENER
    // ==================================================

    function setupAuthListener() {

        const client = getClient();

        if (!client) {
            return;
        }


        client.auth.onAuthStateChange(
            function (event, session) {

                console.log(
                    "TaskEarn auth event:",
                    event
                );

                window.dispatchEvent(
                    new CustomEvent(
                        "taskearn:authchange",
                        {
                            detail: {
                                event: event,
                                session: session
                            }
                        }
                    )
                );

            }
        );

    }


    // ==================================================
    // START AUTH LISTENER
    // ==================================================

    if (
        document.readyState ===
        "loading"
    ) {

        document.addEventListener(
            "DOMContentLoaded",
            setupAuthListener
        );

    } else {

        setupAuthListener();

    }


})();
