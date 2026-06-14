(function () {
    const deptSelect = document.getElementById('assign-dept');
    const courseSelect = document.getElementById('assign-course');
    if (!deptSelect || !courseSelect || !Array.isArray(assignableCourses)) return;

    function renderCourses(departmentCode) {
        courseSelect.innerHTML = '';
        const placeholder = document.createElement('option');
        placeholder.value = '';
        placeholder.textContent = departmentCode ? 'Select course' : 'Select department first';
        courseSelect.appendChild(placeholder);

        const filtered = assignableCourses.filter(c => !departmentCode || c.departmentCode === departmentCode);
        filtered.forEach(course => {
            const option = document.createElement('option');
            option.value = course.id;
            option.textContent = `${course.courseCode} — ${course.courseName}`;
            courseSelect.appendChild(option);
        });

        courseSelect.disabled = !departmentCode || filtered.length === 0;
        if (departmentCode && filtered.length === 0) {
            placeholder.textContent = 'No unassigned courses in this department';
        }
    }

    deptSelect.addEventListener('change', () => renderCourses(deptSelect.value));
    renderCourses('');
})();
