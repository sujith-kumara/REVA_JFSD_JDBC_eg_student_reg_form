// Validate input fields before sending to server
function validateForm() {
  const name = document.getElementById("name").value.trim();
  const srn = document.getElementById("srn").value.trim();

  if (name.length < 3) {
    alert("Name must be at least 3 characters long!");
    return false;
  }
  if (!/^SRN\d+$/i.test(srn)) {
    alert("SRN must start with SRN followed by numbers (e.g., SRN001)");
    return false;
  }
  return true;
}
