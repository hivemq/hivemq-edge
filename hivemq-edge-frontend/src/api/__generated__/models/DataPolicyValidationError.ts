/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { AtMostOneFunctionValidationError } from './AtMostOneFunctionValidationError';
import type { EmptyFieldValidationError } from './EmptyFieldValidationError';
import type { FunctionMustBePairedValidationError } from './FunctionMustBePairedValidationError';
import type { InvalidFieldLengthValidationError } from './InvalidFieldLengthValidationError';
import type { InvalidFieldValueValidationError } from './InvalidFieldValueValidationError';
import type { InvalidFunctionOrderValidationError } from './InvalidFunctionOrderValidationError';
import type { InvalidIdentifierValidationError } from './InvalidIdentifierValidationError';
import type { InvalidSchemaVersionValidationError } from './InvalidSchemaVersionValidationError';
import type { MissingFieldValidationError } from './MissingFieldValidationError';
import type { UnknownVariableValidationError } from './UnknownVariableValidationError';
import type { UnsupportedFieldValidationError } from './UnsupportedFieldValidationError';
import type { ValidationError } from './ValidationError';

export type DataPolicyValidationError = (ValidationError & (AtMostOneFunctionValidationError | FunctionMustBePairedValidationError | InvalidFunctionOrderValidationError | InvalidSchemaVersionValidationError | UnknownVariableValidationError | EmptyFieldValidationError | InvalidFieldLengthValidationError | InvalidFieldValueValidationError | InvalidIdentifierValidationError | MissingFieldValidationError | UnsupportedFieldValidationError));

