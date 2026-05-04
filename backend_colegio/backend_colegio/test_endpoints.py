import requests

BASE_URL = "http://192.168.101.9:8000"

print("=== Probando endpoints ===\n")

# Test 1: Test alumnos
print("1. Test alumnos endpoint:")
try:
    response = requests.get(f"{BASE_URL}/api/mobile/test-alumnos/")
    print(f"   Status: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"   Total: {data.get('total', 0)}")
        print(f"   Primeros 3: {data.get('data', [])[:3]}")
    else:
        print(f"   Error: {response.text}")
except Exception as e:
    print(f"   Error de conexión: {e}")

print("\n2. Asistencia alumnos endpoint:")
try:
    response = requests.get(f"{BASE_URL}/api/mobile/asistencia/alumnos/")
    print(f"   Status: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"   Total: {data.get('total', 0)}")
        print(f"   Primeros 3: {data.get('data', [])[:3]}")
    else:
        print(f"   Error: {response.text}")
except Exception as e:
    print(f"   Error de conexión: {e}")

print("\n3. Usuarios (alumnos all) endpoint:")
try:
    response = requests.get(f"{BASE_URL}/api/mobile/usuarios/")
    print(f"   Status: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"   Total: {data.get('total', 0)}")
        if data.get('data'):
            primer_alumno = data['data'][0]
            print(f"   Primer alumno: {primer_alumno.get('nombre_completo')}")
            print(f"   Sección: {primer_alumno.get('seccion')}")
            print(f"   Tiene QR: {'Sí' if primer_alumno.get('qr_image') else 'No'}")
    else:
        print(f"   Error: {response.text}")
except Exception as e:
    print(f"   Error de conexión: {e}")

print("\n=== Fin de pruebas ===")
