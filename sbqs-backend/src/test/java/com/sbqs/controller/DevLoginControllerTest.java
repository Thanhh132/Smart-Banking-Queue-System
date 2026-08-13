package com.sbqs.controller;

import com.sbqs.config.DevLoginProperties;
import com.sbqs.service.DevLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

class DevLoginControllerTest {
    @Test
    void disabledEndpointIsUnavailableForLoopbackAndRemoteRequests() {
        DevLoginService service = mock(DevLoginService.class);
        DevLoginController controller = new DevLoginController(service, new DevLoginProperties());

        assertEquals(404, controller.accounts(request("127.0.0.1", "localhost")).getStatusCode().value());
        assertEquals(404, controller.accounts(request("203.0.113.1", "example.com")).getStatusCode().value());
        verifyNoInteractions(service);
    }

    @Test
    void enabledEndpointAllowsOnlyDirectLoopbackRequests() {
        DevLoginProperties properties = new DevLoginProperties();
        properties.setEnabled(true);
        DevLoginService service = mock(DevLoginService.class);
        DevLoginController controller = new DevLoginController(service, properties);

        assertEquals(200, controller.accounts(request("127.0.0.1", "localhost")).getStatusCode().value());
        verify(service).accounts();

        MockHttpServletRequest forwarded = request("127.0.0.1", "localhost");
        forwarded.addHeader("X-Forwarded-For", "203.0.113.1");
        assertEquals(404, controller.accounts(forwarded).getStatusCode().value());

        MockHttpServletRequest rfcForwarded = request("127.0.0.1", "localhost");
        rfcForwarded.addHeader("Forwarded", "for=203.0.113.1");
        assertEquals(404, controller.accounts(rfcForwarded).getStatusCode().value());

        assertEquals(404, controller.accounts(request("203.0.113.1", "example.com")).getStatusCode().value());
    }

    @Test
    void loginUsesServiceOnlyWhenEnabledOnLoopback() {
        DevLoginProperties properties = new DevLoginProperties();
        properties.setEnabled(true);
        DevLoginService service = mock(DevLoginService.class);
        DevLoginController controller = new DevLoginController(service, properties);

        assertEquals(200, controller.login(7L, request("::1", "::1")).getStatusCode().value());
        verify(service).login(7L);
    }

    private MockHttpServletRequest request(String remoteAddress, String serverName) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        request.setServerName(serverName);
        return request;
    }
}
