export interface Service {
  serviceId: number;
  serviceCode: string;
  serviceName: string;
  serviceType: string;
  description?: string | null;
  estimatedTime: number;
  status: string;
  requiredCustomerFields?: string[];
  formSchema?: FormFieldDefinition[];
  branch?: { branchId: number };
}

export type FormFieldType = 'TEXT' | 'TEXTAREA' | 'NUMBER' | 'DATE' | 'SELECT' | 'RADIO' | 'CHECKBOX';

export interface FormFieldDefinition {
  key: string;
  label: string;
  type: FormFieldType;
  required: boolean;
  placeholder: string;
  section: string;
  options: string[];
}
