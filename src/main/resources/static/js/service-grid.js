(function () {
    'use strict';

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

    // Touch: first tap reveals the description, second tap navigates
    document.querySelectorAll('.service-btn').forEach(function (btn) {
        var moved = false;

        btn.addEventListener('touchstart', function () {
            moved = false;
        }, { passive: true });

        btn.addEventListener('touchmove', function () {
            moved = true;
        }, { passive: true });

        btn.addEventListener('touchend', function (e) {
            if (moved) return;                                    // treat as scroll, not a tap
            if (btn.classList.contains('is-revealed')) return;    // already revealed → let navigation happen
            e.preventDefault();                                    // first tap: reveal instead of navigate
            revealBtn(btn);
        });
    });

    document.addEventListener('touchstart', function (e) {
        if (!e.target.closest('.service-btn')) {
            document.querySelectorAll('.service-btn.is-revealed').forEach(function (b) {
                b.classList.remove('is-revealed');
            });
        }
    }, { passive: true });

    // Audience toggle: swap business/individuals grids and flip the button label
    var toggle = document.getElementById('audience-toggle');
    if (toggle) {
        var grids = {
            business: document.querySelector('[data-audience="business"]'),
            individuals: document.querySelector('[data-audience="individuals"]')
        };
        toggle.addEventListener('click', function () {
            var showingBusiness = !grids.business.hidden;
            var next = showingBusiness ? 'individuals' : 'business';
            grids.business.hidden = (next !== 'business');
            grids.individuals.hidden = (next !== 'individuals');
            toggle.textContent = showingBusiness
                ? toggle.dataset.labelBusiness
                : toggle.dataset.labelIndividuals;
            toggle.blur();
        });
    }

}());
