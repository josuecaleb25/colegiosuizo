#!/usr/bin/env bash
# exit on error
set -o errexit

# Instalar dependencias
pip install -r requirements.txt

# Navegar al directorio del proyecto Django
cd backend_colegio

# Recolectar archivos estáticos
python manage.py collectstatic --no-input

# Ejecutar migraciones
python manage.py migrate

# Cargar datos iniciales (seed)
# IMPORTANTE: Si usas SQLite, esto se ejecutará en cada deploy
python manage.py seed || echo "Seed ya ejecutado o error al cargar datos"

echo "✅ Build completado exitosamente"
