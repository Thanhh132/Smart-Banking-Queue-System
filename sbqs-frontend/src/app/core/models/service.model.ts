export interface Service {
  serviceId: number;
  serviceCode: string;
  serviceName: string;
  serviceType: string;
  description?: string | null;
  estimatedTime: number;
  status: string;
  requiredCustomerFields?: string[];
}
