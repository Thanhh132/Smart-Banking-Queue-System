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

export interface ServiceCatalogItem {
  catalogId: number;
  serviceCode: string;
  serviceName: string;
  serviceType: string;
  description?: string | null;
  estimatedTime: number;
  status: string;
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
