import deviceTokensRepository from './device-tokens.repository';

class DeviceTokensService {
  async register(personaId: string, token: string, deviceInfo = '') {
    const student = await deviceTokensRepository.findStudentByPersona(personaId);
    const userId = await deviceTokensRepository.findUserId(personaId);

    await deviceTokensRepository.removePreviousAssociations(token);
    return deviceTokensRepository.save({
      userId,
      personaId,
      studentId: student?.id || null,
      token,
      deviceInfo
    });
  }
}

export default new DeviceTokensService();
