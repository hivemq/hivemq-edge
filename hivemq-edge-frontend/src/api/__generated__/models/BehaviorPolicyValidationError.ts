/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { AtLeastOneFieldMissingValidationError } from './AtLeastOneFieldMissingValidationError';
import type { EmptyFieldValidationError } from './EmptyFieldValidationError';
import type { IllegalEventTransitionValidationError } from './IllegalEventTransitionValidationError';
import type { IllegalFunctionValidationError } from './IllegalFunctionValidationError';
import type { InvalidFieldLengthValidationError } from './InvalidFieldLengthValidationError';
import type { InvalidFieldValueValidationError } from './InvalidFieldValueValidationError';
import type { InvalidIdentifierValidationError } from './InvalidIdentifierValidationError';
import type { InvalidSchemaVersionValidationError } from './InvalidSchemaVersionValidationError';
import type { MissingFieldValidationError } from './MissingFieldValidationError';
import type { UnsupportedFieldValidationError } from './UnsupportedFieldValidationError';
import type { ValidationError } from './ValidationError';

export type BehaviorPolicyValidationError = (ValidationError & (IllegalEventTransitionValidationError | IllegalFunctionValidationError | InvalidSchemaVersionValidationError | AtLeastOneFieldMissingValidationError | EmptyFieldValidationError | InvalidFieldLengthValidationError | InvalidFieldValueValidationError | InvalidIdentifierValidationError | MissingFieldValidationError | UnsupportedFieldValidationError));

