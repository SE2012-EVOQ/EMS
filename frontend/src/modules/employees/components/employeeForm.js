export function createEmployeePayload(formData) {
  return {
    firstName: formData.firstName.trim(),
    lastName: formData.lastName.trim(),
    email: formData.email.trim(),
    phone: formData.phone.trim() || null,
    address: formData.address.trim() || null,
    jobTitle: formData.jobTitle.trim(),
    hireDate: formData.hireDate,
    departmentId: Number(formData.departmentId),
    teamId: formData.teamId ? Number(formData.teamId) : null,
    supervisorId: formData.supervisorId ? Number(formData.supervisorId) : null,
    status: formData.status,
    createAccount: formData.createAccount,
    username: formData.createAccount ? formData.username.trim() : null,
    password: formData.createAccount ? formData.password : null,
    role: formData.createAccount ? formData.role : null
  }
}
