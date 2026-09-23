// ==========================================
// TASKEARN - CLEAN APP CONTROLLER
// ==========================================

"use strict";

let userBalance = 0;


// ==========================================
// PAGE LOAD
// ==========================================

document.addEventListener("DOMContentLoaded", function () {

    console.log("TaskEarn loaded successfully.");

    if (typeof testSupabaseConnection === "function") {
        testSupabaseConnection();
    }

});


// ==========================================
// TASK NAVIGATION
// ==========================================

function openTasks() {

    const tasks = document.getElementById("tasks");

    if (tasks) {
        tasks.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    }

}

function goToTasks() {

    openTasks();

}

function showAllTasks() {

    openTasks();

}


// ==========================================
// DOWNLOAD APP
// ==========================================

function downloadApp() {

    const apkPath = "downloads/TaskEarn.apk";

    const link = document.createElement("a");

    link.href = apkPath;
    link.download = "TaskEarn.apk";

    document.body.appendChild(link);

    link.click();

    link.remove();

}


// ==========================================
// EMPTY TASK
// ==========================================

function showNoTasks() {

    alert(
        "No tasks are available right now.\n\n" +
        "New eligible tasks will appear here."
    );

}


// ==========================================
// SUPABASE USER
// ==========================================

async function getCurrentUser() {

    try {

        if (
            typeof supabaseClient === "undefined" ||
            !supabaseClient
        ) {
            return null;
        }

        const {
            data,
            error
        } = await supabaseClient.auth.getUser();

        if (error || !data || !data.user) {
            return null;
        }

        return data.user;

    } catch (error) {

        console.error(
            "User check failed:",
            error
        );

        return null;
    }

}


// ==========================================
// LOGIN REQUIRED
// ==========================================

async function requireLogin() {

    const user = await getCurrentUser();

    if (!user) {

        if (
            typeof window.openLogin === "function"
        ) {

            window.openLogin();

        } else {

            alert(
                "Please login to continue."
            );

        }

        return null;
    }

    return user;

}


// ==========================================
// START EARNING
// ==========================================

async function startEarning() {

    const user = await requireLogin();

    if (!user) {
        return;
    }

    openTasks();

}


// ==========================================
// WALLET
// ==========================================

async function openWallet() {

    const user = await requireLogin();

    if (!user) {
        return;
    }

    const old =
        document.getElementById(
            "walletModal"
        );

    if (old) {
        old.remove();
    }

    const modal =
        document.createElement("div");

    modal.id = "walletModal";

    modal.innerHTML = `

        <div class="task-modal-overlay">

            <div class="task-modal-card">

                <button
                    type="button"
                    class="task-modal-close"
                    onclick="closeWallet()">
                    ×
                </button>

                <div class="task-modal-icon">
                    ₹
                </div>

                <span class="task-modal-badge">
                    TASK EARN WALLET
                </span>

                <h2>
                    Your Wallet
                </h2>

                <p class="task-modal-description">
                    Your verified TaskEarn rewards
                    will appear here.
                </p>

                <div class="task-modal-info">

                    <div>

                        <span>₹</span>

                        <strong>
                            ₹${Number(
                                userBalance
                            ).toFixed(2)}
                        </strong>

                        <small>
                            Available Balance
                        </small>

                    </div>

                    <div>

                        <span>✓</span>

                        <strong>
                            Secure
                        </strong>

                        <small>
                            Account
                        </small>

                    </div>

                </div>

                <button
                    type="button"
                    class="task-start-button"
                    onclick="closeWallet()">

                    Close

                </button>

            </div>

        </div>

    `;

    document.body.appendChild(modal);

}


function closeWallet() {

    const modal =
        document.getElementById(
            "walletModal"
        );

    if (modal) {
        modal.remove();
    }

}


// ==========================================
// TASK MODAL
// ==========================================

function openTask(
    title,
    description,
    duration,
    reward
) {

    const old =
        document.getElementById(
            "taskModal"
        );

    if (old) {
        old.remove();
    }

    const modal =
        document.createElement("div");

    modal.id = "taskModal";

    modal.innerHTML = `

        <div class="task-modal-overlay">

            <div class="task-modal-card">

                <button
                    type="button"
                    class="task-modal-close"
                    onclick="closeTaskModal()">
                    ×
                </button>

                <div class="task-modal-icon">

                    <span class="task-icon-symbol">
                        TASK
                    </span>

                </div>

                <span class="task-modal-badge">
                    ✓ VERIFIED TASK
                </span>

                <h2>
                    ${escapeHTML(title)}
                </h2>

                <p class="task-modal-description">
                    ${escapeHTML(description)}
                </p>

                <div class="task-modal-info">

                    <div>

                        <span>⏱️</span>

                        <strong>
                            ${escapeHTML(duration)}
                        </strong>

                        <small>
                            Estimated Time
                        </small>

                    </div>

                    <div>

                        <span>💰</span>

                        <strong>
                            ${escapeHTML(reward)}
                        </strong>

                        <small>
                            Task Reward
                        </small>

                    </div>

                </div>

                <button
                    type="button"
                    class="task-start-button"
                    onclick="
                        startTask(
                            '${escapeJS(title)}',
                            '${escapeJS(reward)}'
                        )
                    ">

                    Start Task

                </button>

                <p class="task-modal-note">
                    Complete the task honestly
                    and submit genuine proof.
                </p>

            </div>

        </div>

    `;

    document.body.appendChild(modal);

}


// ==========================================
// CLOSE TASK
// ==========================================

function closeTaskModal() {

    const modal =
        document.getElementById(
            "taskModal"
        );

    if (modal) {
        modal.remove();
    }

}


// ==========================================
// START TASK
// ==========================================

async function startTask(
    title,
    reward
) {

    const user = await requireLogin();

    if (!user) {
        return;
    }

    closeTaskModal();

    alert(
        "Task started!\n\n" +
        title +
        "\nReward: " +
        reward
    );

}


// ==========================================
// SAFE HTML
// ==========================================

function escapeHTML(value) {

    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");

}


// ==========================================
// SAFE JAVASCRIPT STRING
// ==========================================

function escapeJS(value) {

    return String(value ?? "")
        .replace(/\\/g, "\\\\")
        .replace(/'/g, "\\'")
        .replace(/"/g, '\\"')
        .replace(/\n/g, "\\n")
        .replace(/\r/g, "\\r");

}


// ==========================================
// SUPABASE TEST
// ==========================================

async function testSupabaseConnection() {

    if (
        typeof supabaseClient === "undefined" ||
        !supabaseClient
    ) {

        console.log(
            "Supabase client not loaded."
        );

        return;
    }

    try {

        const result =
            await supabaseClient
                .from("wallet_balances")
                .select(
                    "user_id,balance"
                )
                .limit(1);

        if (result.error) {

            console.log(
                "Supabase table test:",
                result.error.message
            );

            return;
        }

        console.log(
            "Supabase connected successfully."
        );

    } catch (error) {

        console.error(
            "Supabase test failed:",
            error
        );

    }

}


// ==========================================
// GLOBAL FUNCTIONS
// ==========================================

window.openTasks = openTasks;
window.goToTasks = goToTasks;
window.showAllTasks = showAllTasks;

window.downloadApp = downloadApp;

window.requireLogin = requireLogin;
window.getCurrentUser = getCurrentUser;

window.startEarning = startEarning;

window.openWallet = openWallet;
window.closeWallet = closeWallet;

window.openTask = openTask;
window.closeTaskModal = closeTaskModal;
window.startTask = startTask;

window.showNoTasks = showNoTasks;