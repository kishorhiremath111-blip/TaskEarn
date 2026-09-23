document.addEventListener("DOMContentLoaded", async () => {

    const taskList = document.getElementById("taskList");
    const loading = document.getElementById("loading");
    const empty = document.getElementById("empty");
    const errorBox = document.getElementById("errorBox");

    if (!taskList) {
        console.error("taskList element not found.");
        return;
    }

    function showError(message) {
        if (loading) loading.style.display = "none";
        if (empty) empty.style.display = "none";

        if (errorBox) {
            errorBox.textContent = message;
            errorBox.style.display = "block";
        }
    }

    function showEmpty() {
        if (loading) loading.style.display = "none";
        if (errorBox) errorBox.style.display = "none";
        if (empty) empty.style.display = "block";
    }

    function createTaskCard(task) {

        const card = document.createElement("div");
        card.className = "task-card";

        const reward = Number(task.reward || 0).toFixed(2);
        const duration = task.duration_minutes || 0;
        const category = task.category || "General";

        card.innerHTML = `
            <div class="task-top">
                <div class="task-icon">📱</div>
                <div class="verified">
                    ${task.verified ? "✓ VERIFIED" : "ACTIVE"}
                </div>
            </div>

            <h3>${escapeHTML(task.title || "Untitled Task")}</h3>

            <p>
                ${escapeHTML(
                    task.description || "Complete this task and earn your reward."
                )}
            </p>

            <div class="task-info">
                <div class="task-time">
                    ⏱ ${duration} min • ${escapeHTML(category)}
                </div>

                <div class="reward">
                    ₹${reward}
                </div>
            </div>

            <button
                class="start-task"
                data-task-id="${task.id}"
                data-task-url="${task.task_url || ""}"
            >
                Start Task
            </button>
        `;

        const button = card.querySelector(".start-task");

        button.addEventListener("click", () => {
            startTask(task);
        });

        return card;
    }

    function escapeHTML(value) {
        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    async function loadTasks() {

        if (loading) loading.style.display = "block";
        if (empty) empty.style.display = "none";
        if (errorBox) errorBox.style.display = "none";

        try {

            const { data, error } = await supabaseClient
                .from("tasks")
                .select("*")
                .eq("status", "active")
                .order("created_at", {
                    ascending: false
                });

            if (error) {
                console.error("Supabase task error:", error);
                showError("Tasks load nahi ho rahe: " + error.message);
                return;
            }

            taskList.innerHTML = "";

            if (!data || data.length === 0) {
                showEmpty();
                return;
            }

            data.forEach(task => {
                taskList.appendChild(createTaskCard(task));
            });

            if (loading) loading.style.display = "none";

        } catch (err) {

            console.error(err);

            showError(
                "Something went wrong: " +
                (err.message || "Unknown error")
            );
        }
    }

    async function startTask(task) {

        if (task.task_url) {

            window.open(
                task.task_url,
                "_blank",
                "noopener,noreferrer"
            );

        } else {

            alert(
                "This task does not have a task link yet."
            );
        }
    }

    await loadTasks();

});