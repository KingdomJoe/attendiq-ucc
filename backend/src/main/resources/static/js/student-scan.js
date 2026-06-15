(function () {
    "use strict";

    var DEVICE_KEY = "attendiq_device_id";
    var pendingToken = null;
    var confirming = false;

    var ERROR_MESSAGES = {
        INVALID_INDEX: "Index number does not match your account.",
        ALREADY_MARKED: "You are already marked present for this session.",
        DEVICE_ALREADY_USED: "This device has already been used to mark attendance in this session.",
        QR_EXPIRED: "This QR code has expired. Ask your lecturer to refresh it.",
        NOT_ENROLLED: "You are not enrolled in this course.",
        QR_CONSUMED: "This QR code has already been used.",
        SESSION_CLOSED: "This attendance session has ended.",
        QR_INVALID: "Invalid QR code. Please scan again."
    };

    function getOrCreateDeviceId() {
        try {
            var existing = localStorage.getItem(DEVICE_KEY);
            if (existing) {
                return existing;
            }
            var id = (window.crypto && crypto.randomUUID)
                ? crypto.randomUUID()
                : "dev-" + Date.now() + "-" + Math.random().toString(36).slice(2);
            localStorage.setItem(DEVICE_KEY, id);
            return id;
        } catch (e) {
            return "dev-" + Date.now();
        }
    }

    function el(id) {
        return document.getElementById(id);
    }

    function showState(state) {
        var states = ["confirm", "success", "error"];
        for (var i = 0; i < states.length; i++) {
            var node = el("scan-modal-" + states[i]);
            if (node) {
                node.classList.toggle("hidden", states[i] !== state);
            }
        }
    }

    function formatTime(iso) {
        if (!iso) return "Just now";
        try {
            return new Date(iso).toLocaleString(undefined, {
                month: "short",
                day: "numeric",
                hour: "numeric",
                minute: "2-digit"
            });
        } catch (e) {
            return "Just now";
        }
    }

    function openModal() {
        var modal = el("scan-modal");
        if (modal) {
            modal.classList.remove("hidden");
            document.body.classList.add("scan-modal-open");
        }
    }

    function closeModal() {
        var modal = el("scan-modal");
        if (modal) {
            modal.classList.add("hidden");
        }
        document.body.classList.remove("scan-modal-open");
        pendingToken = null;
        confirming = false;
        var confirmBtn = el("scan-modal-confirm-btn");
        if (confirmBtn) {
            confirmBtn.disabled = false;
            confirmBtn.textContent = "\u2714 Confirm Attendance";
        }
    }

    function mapError(data) {
        if (data && data.errorCode && ERROR_MESSAGES[data.errorCode]) {
            return ERROR_MESSAGES[data.errorCode];
        }
        if (data && data.message) {
            return data.message;
        }
        return "Could not mark attendance. Please try again.";
    }

    function openConfirm(token) {
        pendingToken = token;
        showState("confirm");
        openModal();
        var input = el("scan-modal-index");
        if (input) {
            input.focus();
            input.select();
        }
    }

    function showError(message) {
        var msgEl = el("scan-modal-error-message");
        if (msgEl) {
            msgEl.textContent = message;
        }
        showState("error");
    }

    function showSuccess(courseCode, attendanceTime) {
        var courseEl = el("scan-modal-success-course");
        var timeEl = el("scan-modal-success-time");
        if (courseEl) {
            courseEl.textContent = courseCode ? "Course: " + courseCode : "";
        }
        if (timeEl) {
            timeEl.textContent = "Marked at " + formatTime(attendanceTime);
        }
        showState("success");
        window.setTimeout(function () {
            window.location.reload();
        }, 2200);
    }

    function confirmAttendance() {
        if (!pendingToken || confirming) {
            return;
        }
        var input = el("scan-modal-index");
        var indexNumber = input ? input.value.trim() : "";
        if (!indexNumber) {
            showError(ERROR_MESSAGES.INVALID_INDEX);
            return;
        }

        confirming = true;
        var confirmBtn = el("scan-modal-confirm-btn");
        if (confirmBtn) {
            confirmBtn.disabled = true;
            confirmBtn.textContent = "Confirming\u2026";
        }

        fetch("/attendance/scan", {
            method: "POST",
            credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                token: pendingToken,
                indexNumber: indexNumber,
                clientDeviceId: getOrCreateDeviceId()
            })
        })
            .then(function (response) {
                return response.text().then(function (text) {
                    var data = {};
                    if (text) {
                        try {
                            data = JSON.parse(text);
                        } catch (e) {
                            data = { message: text };
                        }
                    }
                    return { ok: response.ok, data: data };
                });
            })
            .then(function (result) {
                confirming = false;
                if (confirmBtn) {
                    confirmBtn.disabled = false;
                    confirmBtn.textContent = "\u2714 Confirm Attendance";
                }
                if (result.ok) {
                    showSuccess(result.data.courseCode, result.data.attendanceTime);
                    return;
                }
                showError(mapError(result.data));
            })
            .catch(function () {
                confirming = false;
                if (confirmBtn) {
                    confirmBtn.disabled = false;
                    confirmBtn.textContent = "\u2714 Confirm Attendance";
                }
                showError("Network error. Check your connection and try again.");
            });
    }

    function decodeUploadFile(file) {
        if (!file || typeof Html5Qrcode === "undefined") {
            if (window.showScanPageError) {
                window.showScanPageError("Upload is unavailable. Please use the camera scanner.");
            }
            return;
        }
        Html5Qrcode.scanFile(file, true)
            .then(function (decodedText) {
                openConfirm(decodedText);
            })
            .catch(function () {
                if (window.showScanPageError) {
                    window.showScanPageError("No QR code detected in the image. Please take a clear photo of the QR code.");
                }
            });
    }

    function bindEvents() {
        var confirmBtn = el("scan-modal-confirm-btn");
        if (confirmBtn) {
            confirmBtn.addEventListener("click", confirmAttendance);
        }

        var retryBtn = el("scan-modal-error-retry");
        if (retryBtn) {
            retryBtn.addEventListener("click", function () {
                if (pendingToken) {
                    showState("confirm");
                } else {
                    closeModal();
                }
            });
        }

        var modal = el("scan-modal");
        if (modal) {
            modal.addEventListener("click", function (event) {
                var target = event.target;
                if (target && target.getAttribute && target.getAttribute("data-scan-dismiss") === "true") {
                    closeModal();
                }
            });
        }

        var indexInput = el("scan-modal-index");
        if (indexInput) {
            indexInput.addEventListener("keydown", function (event) {
                if (event.key === "Enter") {
                    event.preventDefault();
                    confirmAttendance();
                }
            });
        }

        var uploadInput = el("qr-upload-input");
        if (uploadInput) {
            uploadInput.addEventListener("change", function () {
                var file = uploadInput.files && uploadInput.files[0];
                uploadInput.value = "";
                if (file) {
                    decodeUploadFile(file);
                }
            });
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", bindEvents);
    } else {
        bindEvents();
    }

    window.StudentScan = {
        openConfirm: openConfirm,
        cancel: closeModal,
        confirm: confirmAttendance
    };
})();
