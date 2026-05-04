#!/usr/bin/env python
"""
Script para generar una SECRET_KEY segura para Django
Ejecuta: python generate_secret_key.py
"""
from django.core.management.utils import get_random_secret_key

if __name__ == '__main__':
    secret_key = get_random_secret_key()
    print("\n" + "="*60)
    print("SECRET_KEY generada para Django:")
    print("="*60)
    print(f"\n{secret_key}\n")
    print("="*60)
    print("\nCopia esta clave y úsala en las variables de entorno de Render")
    print("="*60 + "\n")
