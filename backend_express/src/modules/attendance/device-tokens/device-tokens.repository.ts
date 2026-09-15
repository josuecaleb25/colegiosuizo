import supabase from '../../../config/database';

class DeviceTokensRepository {
  async findStudentByPersona(personaId: string) {
    const { data, error } = await supabase
      .from('alumnos')
      .select('id')
      .eq('persona_id', personaId)
      .maybeSingle();
    if (error) throw error;
    return data;
  }

  async findUserId(personaId: string) {
    const { data, error } = await supabase
      .from('usuarios')
      .select('id')
      .eq('persona_id', personaId)
      .maybeSingle();
    if (error) throw error;
    return data?.id || null;
  }

  async removePreviousAssociations(token: string) {
    const { error } = await supabase
      .from('device_tokens')
      .delete()
      .eq('token', token);
    if (error) throw error;
  }

  async save(input: {
    userId: string | null;
    personaId: string;
    studentId: string | null;
    token: string;
    deviceInfo: string;
  }) {
    const { data, error } = await supabase
      .from('device_tokens')
      .upsert({
        user_id: input.userId,
        persona_id: input.personaId,
        estudiante_id: input.studentId,
        token: input.token,
        device_info: input.deviceInfo,
        updated_at: new Date().toISOString()
      }, { onConflict: 'persona_id,token' })
      .select();
    if (error) throw error;
    return data;
  }
}

export default new DeviceTokensRepository();
