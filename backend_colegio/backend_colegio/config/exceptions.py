from rest_framework.views import exception_handler
from rest_framework.response import Response
from rest_framework import status


def custom_exception_handler(exc, context):
    response = exception_handler(exc, context)

    if response is not None:
        error_response = {
            'success': False,
            'message': _get_error_message(response.data),
            'errors': response.data,
            'status_code': response.status_code,
        }
        response.data = error_response

    return response


def _get_error_message(data):
    if isinstance(data, dict):
        if 'detail' in data:
            return str(data['detail'])
        first_key = next(iter(data))
        first_val = data[first_key]
        if isinstance(first_val, list):
            return str(first_val[0])
        return str(first_val)
    if isinstance(data, list):
        return str(data[0])
    return str(data)
