import { describe, expect, it } from 'vitest';

import { ApiErrorService } from './api-error.service';

describe('ApiErrorService', () => {
  const service = new ApiErrorService();

  it('maps an explicit duplicate phone message', () => {
    expect(service.getMessage({ error: { message: 'Số điện thoại đã tồn tại' } }, 'fallback'))
      .toBe('Số điện thoại đã tồn tại. Vui lòng dùng số khác.');
  });

  it('does not mistake a SQL statement containing the phone column for a duplicate phone', () => {
    const message = 'SQLGrammarException: insert into users (email, phone, identity_provider)';

    expect(service.getMessage({ error: { message } }, 'fallback'))
      .toBe('Cấu trúc dữ liệu hệ thống chưa được cập nhật. Vui lòng khởi động lại backend và thử lại.');
  });
});
