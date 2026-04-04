document.addEventListener('DOMContentLoaded', () => {
    const valueInputs = document.querySelectorAll('input[name="value"]');
    valueInputs.forEach(input => {
        input.addEventListener('input', (e) => {
            let val = e.target.value;
            val = val.replace(/[^0-9,\.]/g, '');
            e.target.value = val;
        });
    });
});
