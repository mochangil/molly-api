package org.example.mollyapi.user.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.mollyapi.user.auth.dto.SignInReqDto;
import org.example.mollyapi.user.auth.dto.SignInResDto;
import org.example.mollyapi.user.auth.service.SignInService;
import org.example.mollyapi.user.type.Role;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;


import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(SignInController.class)
class SignInControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SignInService signInService;

    @Test
    @DisplayName("사용자의 권한을 인증한다.")
    void signIn() throws Exception {
        //given
        String inputPassword = "qwer1234";
        String inputEmail = "test@example.com";

        SignInReqDto request = new SignInReqDto(inputEmail, inputPassword);


        SignInResDto result = new SignInResDto("testAccessToken", List.of(Role.BUY));
        when(signInService.signIn(request))
                .thenReturn(result);


        //when //then
        mockMvc.perform(MockMvcRequestBuilders.post("/sign-in")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", Matchers.startsWith("Bearer")))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("사용자의 권한 인증할 시 이메일은 필수 입니다.")
    void signIn_Without_Email() throws Exception {
        //given
        String inputPassword = "qwer1234";
        String inputEmail = null;

        SignInReqDto request = new SignInReqDto(inputEmail, inputPassword);


        SignInResDto result = new SignInResDto("testAccessToken", List.of(Role.BUY));
        when(signInService.signIn(request))
                .thenReturn(result);


        //when //then
        mockMvc.perform(MockMvcRequestBuilders.post("/sign-in")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("error").value("NOT_PARAM_VALID"))
                .andExpect(jsonPath("email").value("이메일은 필수값 입니다."));
    }

    @Test
    @DisplayName("사용자의 권한 인증할 시 이메일은 반드시 이메일 형식이여야 합니다.")
    void signIn_Without_Email_Format() throws Exception {
        //given
        String inputPassword = "qwer1234";
        String inputEmail = "qwer2341235";

        SignInReqDto request = new SignInReqDto(inputEmail, inputPassword);


        SignInResDto result = new SignInResDto("testAccessToken", List.of(Role.BUY));
        when(signInService.signIn(request))
                .thenReturn(result);


        //when //then
        mockMvc.perform(MockMvcRequestBuilders.post("/sign-in")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("error").value("NOT_PARAM_VALID"))
                .andExpect(jsonPath("email").value("이메일 형식이 아닙니다."));
    }

    @Test
    @DisplayName("사용자의 권한 인증할 시 비밀번호는 필수 입니다.")
    void signIn_Without_Password() throws Exception {
        //given
        String inputPassword = null;
        String inputEmail = "test@example.com";

        SignInReqDto request = new SignInReqDto(inputEmail, inputPassword);


        SignInResDto result = new SignInResDto("testAccessToken", List.of(Role.BUY));
        when(signInService.signIn(request))
                .thenReturn(result);


        //when //then
        mockMvc.perform(MockMvcRequestBuilders.post("/sign-in")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("error").value("NOT_PARAM_VALID"))
                .andExpect(jsonPath("password").value("비밀번호는 필수값 입니다."));
    }


}