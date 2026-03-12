(function () {
    'use strict';

    var LONG_PRESS_MS = 500;

    function revealBtn(btn) {
        var alreadyRevealed = btn.classList.contains('is-revealed');
        // Close any currently open button
        document.querySelectorAll('.service-btn.is-revealed').forEach(function (b) {
            b.classList.remove('is-revealed');
        });
        // Open this one only if it wasn't already open (acts as a toggle)
        if (!alreadyRevealed) {
            btn.classList.add('is-revealed');
        }
    }

    document.querySelectorAll('.service-btn').forEach(function (btn) {
        var timer = null;
        var longPressActivated = false;

        btn.addEventListener('touchstart', function () {
            longPressActivated = false;
            timer = setTimeout(function () {
                longPressActivated = true;
                revealBtn(btn);
            }, LONG_PRESS_MS);
        }, { passive: true });

        // Scrolling cancels the long-press timer
        btn.addEventListener('touchmove', function () {
            clearTimeout(timer);
            timer = null;
        }, { passive: true });

        btn.addEventListener('touchend', function (e) {
            clearTimeout(timer);
            timer = null;

            if (longPressActivated) {
                // Long press fired — block the subsequent click so we don't navigate
                e.preventDefault();
            } else if (btn.classList.contains('is-revealed')) {
                // Tap while description is showing — dismiss instead of navigate
                e.preventDefault();
                btn.classList.remove('is-revealed');
            }
            // Otherwise: normal short tap → navigation proceeds as usual
        });

        btn.addEventListener('touchcancel', function () {
            clearTimeout(timer);
            timer = null;
        }, { passive: true });
    });

    // Dismiss on outside tap
    document.addEventListener('touchstart', function (e) {
        if (!e.target.closest('.service-btn')) {
            document.querySelectorAll('.service-btn.is-revealed').forEach(function (b) {
                b.classList.remove('is-revealed');
            });
        }
    }, { passive: true });

}());
