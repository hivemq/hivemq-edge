/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { InvalidFieldLengthValidationError } from './InvalidFieldLengthValidationError';
import type { InvalidFieldValueValidationError } from './InvalidFieldValueValidationError';
import type { InvalidIdentifierValidationError } from './InvalidIdentifierValidationError';
import type { MissingFieldValidationError } from './MissingFieldValidationError';
import type { UnsupportedFieldValidationError } from './UnsupportedFieldValidationError';
import type { ValidationError } from './ValidationError';

export type ScriptValidationError = (ValidationError & (InvalidFieldLengthValidationError | InvalidFieldValueValidationError | InvalidIdentifierValidationError | MissingFieldValidationError | UnsupportedFieldValidationError));

