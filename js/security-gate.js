/* =========================================================
   TASK EARN — GLOBAL SECURITY GATE
   VPN + PRIVATE DNS + KNOWN AD-BLOCKING DNS
========================================================= */

(function () {

    "use strict";

    const CHECK_INTERVAL = 30000; // 30 seconds

    let lastStatus = null;
    let popupOpen = false;
    let checking = false;

    /* -------------------------------------------------------
       NATIVE BRIDGE
    ------------------------------------------------------- */

    function nativeAvailable() {

        return (
            typeof window.AndroidAds !== "undefined" &&
            typeof window.AndroidAds.getNetworkSecurityStatus === "function"
        );
    }

    /* -------------------------------------------------------
       GET SECURITY STATUS
    ------------------------------------------------------- */

    function getStatus() {

        if (!nativeAvailable()) {

            return {
                native: false,
                vpn: false,
                privateDns: false,
                adBlockingDns: false,
                privateDnsHostname: "",
                blocked: false
            };
        }

        try {

            const raw =
                window.AndroidAds.getNetworkSecurityStatus();

            const data =
                typeof raw === "string"
                    ? JSON.parse(raw)
                    : raw;

            return {
                native: true,
                vpn: !!data.vpn,
                privateDns: !!data.privateDns,
                adBlockingDns: !!data.adBlockingDns,
                privateDnsHostname:
                    data.privateDnsHostname || "",
                blocked: !!data.blocked
            };

        } catch (error) {

            console.error(
                "TaskEarn Security:",
                error
            );

            return {
                native: true,
                vpn: false,
                privateDns: false,
                adBlockingDns: false,
                privateDnsHostname: "",
                blocked: false
            };
        }
    }

    /* -------------------------------------------------------
       REASON
    ------------------------------------------------------- */

    function getReason(status) {

        if (status.vpn) {

            return {
                title: "VPN Detected",
                message:
                    "Please turn off your VPN to continue using TaskEarn."
            };
        }

        if (status.adBlockingDns) {

            return {
                title: "Ad Blocker Detected",
                message:
                    "An ad-blocking Private DNS is active on your device. Turn it off to continue."
            };
        }

        return {
            title: "Network Protection Required",
            message:
                "Please disable network settings that block TaskEarn services."
        };
    }

    /* -------------------------------------------------------
       POPUP
    ------------------------------------------------------- */

    function createPopup() {

        if (document.getElementById(
            "taskearn-security-overlay"
        )) {
            return;
        }

        const overlay =
            document.createElement("div");

        overlay.id =
            "taskearn-security-overlay";

        overlay.innerHTML = `

            <div class="te-security-backdrop"></div>

            <div class="te-security-modal">

                <div class="te-security-icon">

                    <div class="te-security-shield">
                        !
                    </div>

                </div>

                <div class="te-security-content">

                    <div class="te-security-label">
                        SECURITY CHECK
                    </div>

                    <h2 id="te-security-title">
                        Ad Blocker Detected
                    </h2>

                    <p id="te-security-message">
                        Please disable your network blocker
                        to continue using TaskEarn.
                    </p>

                    <div
                        class="te-security-status"
                        id="te-security-status"
                    >
                        <span class="te-status-dot"></span>
                        <span>Network protection active</span>
                    </div>

                    <button
                        type="button"
                        id="te-security-settings"
                        class="te-security-primary"
                    >
                        Open Network Settings
                    </button>

                    <button
                        type="button"
                        id="te-security-recheck"
                        class="te-security-secondary"
                    >
                        Check Again
                    </button>

                    <div class="te-security-note">
                        TaskEarn requires a normal network
                        connection for protected features.
                    </div>

                </div>

            </div>
        `;

        document.body.appendChild(overlay);

        injectStyles();

        document
            .getElementById("te-security-settings")
            .addEventListener(
                "click",
                openSettings
            );

        document
            .getElementById("te-security-recheck")
            .addEventListener(
                "click",
                recheck
            );
    }

    /* -------------------------------------------------------
       POPUP UPDATE
    ------------------------------------------------------- */

    function updatePopup(status) {

        createPopup();

        const reason =
            getReason(status);

        const title =
            document.getElementById(
                "te-security-title"
            );

        const message =
            document.getElementById(
                "te-security-message"
            );

        const statusText =
            document.getElementById(
                "te-security-status"
            );

        if (title) {
            title.textContent =
                reason.title;
        }

        if (message) {
            message.textContent =
                reason.message;
        }

        if (statusText) {

            let text =
                "Network protection active";

            if (status.vpn) {

                text =
                    "VPN connection detected";

            } else if (status.adBlockingDns) {

                text =
                    "Ad-blocking DNS detected";
            }

            statusText.innerHTML = `
                <span class="te-status-dot"></span>
                <span>${text}</span>
            `;
        }

        const overlay =
            document.getElementById(
                "taskearn-security-overlay"
            );

        if (overlay) {

            overlay.classList.add(
                "te-security-visible"
            );

            popupOpen = true;
        }
    }

    /* -------------------------------------------------------
       CLOSE POPUP
    ------------------------------------------------------- */

    function closePopup() {

        const overlay =
            document.getElementById(
                "taskearn-security-overlay"
            );

        if (overlay) {

            overlay.classList.remove(
                "te-security-visible"
            );
        }

        popupOpen = false;
    }

    /* -------------------------------------------------------
       OPEN SETTINGS
    ------------------------------------------------------- */

    function openSettings() {

        try {

            if (
                nativeAvailable() &&
                typeof window.AndroidAds.openPrivateDnsSettings ===
                    "function"
            ) {

                window.AndroidAds
                    .openPrivateDnsSettings();

                return;
            }

            if (
                nativeAvailable() &&
                typeof window.AndroidAds.openNetworkSettings ===
                    "function"
            ) {

                window.AndroidAds
                    .openNetworkSettings();
            }

        } catch (error) {

            console.error(
                "TaskEarn Settings:",
                error
            );
        }
    }

    /* -------------------------------------------------------
       RECHECK
    ------------------------------------------------------- */

    function recheck() {

        if (checking) {
            return;
        }

        checking = true;

        const button =
            document.getElementById(
                "te-security-recheck"
            );

        if (button) {

            button.disabled = true;
            button.textContent =
                "Checking...";
        }

        setTimeout(() => {

            const status =
                getStatus();

            lastStatus =
                status;

            checking = false;

            if (button) {

                button.disabled = false;
                button.textContent =
                    "Check Again";
            }

            if (status.blocked) {

                updatePopup(status);

            } else {

                closePopup();

                showToast(
                    "Security check passed"
                );
            }

        }, 500);
    }

    /* -------------------------------------------------------
       MAIN SECURITY CHECK
    ------------------------------------------------------- */

    function check(options) {

        options =
            options || {};

        const status =
            getStatus();

        lastStatus =
            status;

        /*
         * Browser/Acode preview:
         * native detector unavailable,
         * so don't block development preview.
         */

        if (!status.native) {

            return true;
        }

        if (status.blocked) {

            updatePopup(status);

            return false;
        }

        return true;
    }

    /* -------------------------------------------------------
       PROTECTED ACTION
    ------------------------------------------------------- */

    function protect(action) {

        if (!check()) {
            return false;
        }

        if (
            typeof action === "function"
        ) {

            action();
        }

        return true;
    }

    /* -------------------------------------------------------
       AUTOMATIC PROTECTED BUTTONS
       
       Add:
       data-security-gate="true"
       
       to any button/link that must be protected.
    ------------------------------------------------------- */

    function bindProtectedElements() {

        const elements =
            document.querySelectorAll(
                "[data-security-gate='true']"
            );

        elements.forEach(function (element) {

            if (
                element.dataset
                    .securityGateBound === "true"
            ) {
                return;
            }

            element.dataset
                .securityGateBound = "true";

            element.addEventListener(
                "click",
                function (event) {

                    if (!check()) {

                        event.preventDefault();
                        event.stopImmediatePropagation();

                        return false;
                    }

                },
                true
            );
        });
    }

    /* -------------------------------------------------------
       AUTO CHECK CURRENT PAGE
    ------------------------------------------------------- */

    function shouldProtectCurrentPage() {

        const path =
            window.location.pathname
                .toLowerCase();

        const file =
            path.split("/").pop();

        const protectedPages = [

            "rewards.html",

            "tasks.html",
            "task.html",
            "task-details.html",
            "task-submit.html",

            "projects.html",
            "project-details.html",
            "project-submit.html",

            "submission-status.html",
            "submission-status.html",

            "earnings.html",
            "history.html"
        ];

        return protectedPages.includes(
            file
        );
    }

    /* -------------------------------------------------------
       TOAST
    ------------------------------------------------------- */

    function showToast(message) {

        let toast =
            document.getElementById(
                "taskearn-security-toast"
            );

        if (!toast) {

            toast =
                document.createElement("div");

            toast.id =
                "taskearn-security-toast";

            document.body.appendChild(
                toast
            );
        }

        toast.textContent =
            message;

        toast.classList.add(
            "te-toast-show"
        );

        setTimeout(() => {

            toast.classList.remove(
                "te-toast-show"
            );

        }, 2200);
    }

    /* -------------------------------------------------------
       CSS
    ------------------------------------------------------- */

    function injectStyles() {

        if (
            document.getElementById(
                "taskearn-security-style"
            )
        ) {
            return;
        }

        const style =
            document.createElement("style");

        style.id =
            "taskearn-security-style";

        style.textContent = `

            #taskearn-security-overlay {

                position: fixed;
                inset: 0;
                z-index: 2147483647;

                display: flex;
                align-items: center;
                justify-content: center;

                padding: 22px;

                opacity: 0;
                visibility: hidden;

                transition:
                    opacity .22s ease,
                    visibility .22s ease;
            }

            #taskearn-security-overlay
            .te-security-backdrop {

                position: absolute;
                inset: 0;

                background:
                    rgba(1, 7, 18, .86);

                backdrop-filter:
                    blur(14px);

                -webkit-backdrop-filter:
                    blur(14px);
            }

            #taskearn-security-overlay
            .te-security-modal {

                position: relative;

                width: min(
                    100%,
                    430px
                );

                border-radius: 26px;

                padding: 28px 24px 24px;

                background:
                    linear-gradient(
                        145deg,
                        #0c1d31,
                        #07111f
                    );

                border:
                    1px solid
                    rgba(
                        77,
                        179,
                        255,
                        .22
                    );

                box-shadow:
                    0 30px 90px
                    rgba(
                        0,
                        0,
                        0,
                        .65
                    );

                transform:
                    translateY(16px)
                    scale(.97);

                transition:
                    transform .25s ease;
            }

            #taskearn-security-overlay
            .te-security-icon {

                width: 72px;
                height: 72px;

                margin: 0 auto 20px;

                border-radius: 22px;

                display: flex;
                align-items: center;
                justify-content: center;

                background:
                    linear-gradient(
                        135deg,
                        rgba(
                            37,
                            99,
                            235,
                            .28
                        ),
                        rgba(
                            14,
                            165,
                            233,
                            .16
                        )
                    );

                border:
                    1px solid
                    rgba(
                        96,
                        165,
                        250,
                        .28
                    );
            }

            .te-security-shield {

                width: 42px;
                height: 42px;

                display: flex;
                align-items: center;
                justify-content: center;

                border-radius: 14px;

                background:
                    linear-gradient(
                        135deg,
                        #38bdf8,
                        #6366f1
                    );

                color: white;

                font-size: 23px;
                font-weight: 800;

                box-shadow:
                    0 8px 28px
                    rgba(
                        56,
                        189,
                        248,
                        .28
                    );
            }

            #taskearn-security-overlay
            .te-security-content {

                text-align: center;
            }

            .te-security-label {

                font-size: 11px;

                font-weight: 800;

                letter-spacing: 2px;

                color:
                    #67d8ff;

                margin-bottom: 8px;
            }

            #taskearn-security-overlay h2 {

                margin: 0 0 10px;

                color:
                    #f5f9ff;

                font-size: 24px;

                line-height: 1.2;
            }

            #taskearn-security-overlay p {

                margin: 0 auto 18px;

                max-width: 340px;

                color:
                    #91a4bc;

                font-size: 14px;

                line-height: 1.65;
            }

            .te-security-status {

                display: flex;

                align-items: center;
                justify-content: center;

                gap: 8px;

                margin-bottom: 18px;

                padding: 10px 13px;

                border-radius: 12px;

                background:
                    rgba(
                        15,
                        23,
                        42,
                        .75
                    );

                color:
                    #a9bad0;

                font-size: 12px;

                border:
                    1px solid
                    rgba(
                        148,
                        163,
                        184,
                        .12
                    );
            }

            .te-status-dot {

                width: 7px;
                height: 7px;

                border-radius: 50%;

                background:
                    #f59e0b;

                box-shadow:
                    0 0 10px
                    rgba(
                        245,
                        158,
                        11,
                        .6
                    );
            }

            .te-security-primary,
            .te-security-secondary {

                width: 100%;

                border: 0;

                border-radius: 14px;

                padding: 14px 16px;

                font-size: 14px;

                font-weight: 750;

                cursor: pointer;

                transition:
                    transform .15s ease,
                    opacity .15s ease;
            }

            .te-security-primary {

                color: white;

                background:
                    linear-gradient(
                        135deg,
                        #0ea5e9,
                        #6366f1
                    );

                box-shadow:
                    0 10px 30px
                    rgba(
                        14,
                        165,
                        233,
                        .18
                    );
            }

            .te-security-secondary {

                margin-top: 10px;

                color:
                    #b8c8db;

                background:
                    rgba(
                        30,
                        49,
                        75,
                        .75
                    );

                border:
                    1px solid
                    rgba(
                        96,
                        165,
                        250,
                        .16
                    );
            }

            .te-security-primary:active,
            .te-security-secondary:active {

                transform:
                    scale(.98);
            }

            .te-security-primary:disabled,
            .te-security-secondary:disabled {

                opacity: .55;
            }

            .te-security-note {

                margin-top: 17px;

                color:
                    #64758c;

                font-size: 10px;

                line-height: 1.5;
            }

            #taskearn-security-overlay
            .te-security-modal {

                overflow: hidden;
            }

            #taskearn-security-overlay
            .te-security-modal::before {

                content: "";

                position: absolute;

                width: 180px;
                height: 180px;

                top: -100px;
                right: -80px;

                border-radius: 50%;

                background:
                    rgba(
                        56,
                        189,
                        248,
                        .09
                    );

                filter: blur(5px);

                pointer-events: none;
            }

            #taskearn-security-overlay
            .te-security-modal > * {

                position: relative;
                z-index: 1;
            }

            #taskearn-security-overlay
            .te-security-backdrop {

                pointer-events: auto;
            }

            #taskearn-security-overlay
            .te-security-modal {

                pointer-events: auto;
            }

            #taskearn-security-overlay
            &.te-security-visible {

                opacity: 1;
                visibility: visible;
            }

            #taskearn-security-overlay
            &.te-security-visible
            .te-security-modal {

                transform:
                    translateY(0)
                    scale(1);
            }

            #taskearn-security-toast {

                position: fixed;

                left: 50%;

                bottom: 28px;

                transform:
                    translate(-50%, 20px);

                z-index:
                    2147483646;

                padding:
                    11px 17px;

                border-radius: 12px;

                background:
                    rgba(
                        12,
                        29,
                        49,
                        .96
                    );

                border:
                    1px solid
                    rgba(
                        56,
                        189,
                        248,
                        .25
                    );

                color:
                    #e7f6ff;

                font-size: 12px;

                font-weight: 700;

                opacity: 0;

                pointer-events: none;

                transition:
                    opacity .2s ease,
                    transform .2s ease;
            }

            #taskearn-security-toast
            .te-toast-show {

                opacity: 1;

                transform:
                    translate(-50%, 0);
            }
        `;

        document.head.appendChild(
            style
        );
    }

    /* -------------------------------------------------------
       INITIALIZE
    ------------------------------------------------------- */

    function init() {

        injectStyles();

        bindProtectedElements();

        /*
         * Protected pages:
         * automatically check once when opened.
         */

        if (
            shouldProtectCurrentPage()
        ) {

            setTimeout(
                function () {

                    check();

                },
                500
            );
        }

        /*
         * Re-bind dynamically created
         * buttons/links.
         */

        const observer =
            new MutationObserver(
                function () {

                    bindProtectedElements();

                }
            );

        if (document.body) {

            observer.observe(
                document.body,
                {
                    childList: true,
                    subtree: true
                }
            );
        }

        /*
         * Periodic security check.
         */

        setInterval(
            function () {

                const status =
                    getStatus();

                lastStatus =
                    status;

                if (
                    status.native &&
                    status.blocked &&
                    shouldProtectCurrentPage()
                ) {

                    if (!popupOpen) {

                        updatePopup(
                            status
                        );
                    }
                }

            },
            CHECK_INTERVAL
        );
    }

    /* -------------------------------------------------------
       GLOBAL API
    ------------------------------------------------------- */

    window.TaskEarnSecurity = {

        check: check,

        protect: protect,

        getStatus: getStatus,

        openSettings: openSettings,

        recheck: recheck,

        isBlocked: function () {

            return getStatus()
                .blocked;
        }
    };

    /* -------------------------------------------------------
       START
    ------------------------------------------------------- */

    if (
        document.readyState ===
        "loading"
    ) {

        document.addEventListener(
            "DOMContentLoaded",
            init
        );

    } else {

        init();
    }

})();