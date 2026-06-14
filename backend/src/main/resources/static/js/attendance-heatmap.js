(function () {
    const cache = new Map();

    function formatDate(iso) {
        const date = new Date(iso);
        return date.toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    }

    function renderHeatmap(container, grid) {
        container.innerHTML = '';
        if (!grid.sessions || grid.sessions.length === 0) {
            container.innerHTML = '<p class="text-xs text-muted">No sessions held yet.</p>';
            return;
        }

        grid.sessions.forEach(cell => {
            const square = document.createElement('div');
            square.className = 'attendance-cell ' + (cell.present ? 'attendance-cell--present' : 'attendance-cell--absent');
            square.title = `${cell.present ? 'Present' : 'Absent'} — ${formatDate(cell.sessionDate)}`;
            container.appendChild(square);
        });
    }

    async function loadGrid(courseId, studentId) {
        const key = `${courseId}:${studentId}`;
        if (cache.has(key)) return cache.get(key);

        const response = await fetch(`/courses/${courseId}/students/${studentId}/attendance-grid`, {
            credentials: 'same-origin'
        });
        if (!response.ok) throw new Error('Could not load attendance history');
        const data = await response.json();
        cache.set(key, data);
        return data;
    }

    function closeAllExcept(studentId) {
        document.querySelectorAll('.roster-expand-row').forEach(row => {
            if (row.dataset.expandFor !== String(studentId)) {
                row.classList.add('hidden');
            }
        });
        document.querySelectorAll('.roster-row').forEach(row => row.classList.remove('roster-row--open'));
    }

    document.querySelectorAll('.roster-row').forEach(row => {
        row.addEventListener('click', async () => {
            const studentId = row.dataset.studentId;
            const courseId = row.dataset.courseId;
            const expandRow = document.querySelector(`[data-expand-for="${studentId}"]`);
            if (!expandRow) return;

            const isOpen = !expandRow.classList.contains('hidden');
            closeAllExcept('');
            if (isOpen) {
                expandRow.classList.add('hidden');
                row.classList.remove('roster-row--open');
                return;
            }

            closeAllExcept(studentId);
            expandRow.classList.remove('hidden');
            row.classList.add('roster-row--open');
            expandRow.classList.add('attendance-expand-row--opening');

            const summary = expandRow.querySelector('.attendance-expand-summary');
            const heatmap = document.getElementById(`heatmap-${studentId}`);

            try {
                const grid = await loadGrid(courseId, studentId);
                summary.textContent = `${grid.presentCount} of ${grid.totalSessions} sessions attended`;
                renderHeatmap(heatmap, grid);
            } catch (error) {
                summary.textContent = error.message;
            } finally {
                requestAnimationFrame(() => expandRow.classList.remove('attendance-expand-row--opening'));
            }
        });
    });
})();
