export function isPasswordChangeValid(currentPassword: string, newPassword: string) {
  return currentPassword.length > 0 && currentPassword.length <= 256 && newPassword.length >= 12 && newPassword.length <= 256 && currentPassword !== newPassword;
}
