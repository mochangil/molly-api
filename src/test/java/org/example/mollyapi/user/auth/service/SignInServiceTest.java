package org.example.mollyapi.user.auth.service;

import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.user.auth.config.PasswordEncoder;
import org.example.mollyapi.user.auth.dto.SignInReqDto;
import org.example.mollyapi.user.auth.dto.SignInResDto;
import org.example.mollyapi.user.auth.entity.Auth;
import org.example.mollyapi.user.auth.entity.Password;
import org.example.mollyapi.user.auth.repository.AuthRepository;
import org.example.mollyapi.user.entity.User;
import org.example.mollyapi.user.repository.UserRepository;
import org.example.mollyapi.user.type.Role;
import org.example.mollyapi.user.type.Sex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class SignInServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SignInService signInService;

    @Test
    @DisplayName("회원가입하지 않은 사용자는 로그인할 수 없다.")
    void signIn_NotFoundUser() {
        //given
        String inputEmail = "test@example.com";
        String inputPassword = "qwer1234";

        SignInReqDto testUser = new SignInReqDto(inputEmail, inputPassword);

        //when//then
        assertThatThrownBy( ( ) -> signInService.signIn(testUser))
                .isInstanceOf(CustomException.class)
                .hasMessage("비밀번호 혹은 아이디가 일치하지 않습니다.");


    }

    @Test
    @DisplayName("비밀번호 불일치시 로그인할 수 없다")
    void signIn_Not_Match_Password() {
        //given
        String inputPassword = "qwer1234";
        String inputEmail = "test@example.com";

        User givenUser = userRepository.save(createUser());
        Password givenPassword = createPassword(inputPassword);
        Auth givenAuth = createAuth(givenPassword, givenUser, inputEmail);

        authRepository.save(givenAuth);

        String wrongPassword = "wrong1234";
        SignInReqDto testUser = new SignInReqDto("test@example.com", wrongPassword);
        LocalDateTime now = LocalDateTime.now();

        //when//then
        assertThatThrownBy( ( ) -> signInService.signIn(testUser))
                .isInstanceOf(CustomException.class)
                .hasMessage("비밀번호 혹은 아이디가 일치하지 않습니다.");


    }


    @Test
    @DisplayName("사용자는 로그인 할 수 있다.")
    void signIn() {
        //given
        String inputPassword = "qwer1234";
        String inputEmail = "test@example.com";

        User givenUser = userRepository.save(createUser());
        Password givenPassword = createPassword(inputPassword);
        Auth givenAuth = createAuth(givenPassword, givenUser, inputEmail);

        Auth savedAuth = authRepository.save(givenAuth);

        LocalDateTime now = LocalDateTime.now();
        SignInReqDto testUser = new SignInReqDto("test@example.com", "qwer1234");


        //when
        SignInResDto signInResDto = signInService.signIn(testUser);

        //then
        assertThat(signInResDto.accessToken()).isNotNull();
        assertThat(signInResDto.roles()).containsExactly(Role.BUY);
    }

    private Auth createAuth(Password password, User user, String email) {
        return Auth.builder()
                .email(email)
                .role(List.of(Role.BUY))
                .password(password)
                .user(user)
                .build();
    }

    private User createUser() {
        return User.builder()
                .sex(Sex.FEMALE)
                .nickname("꽃달린감나무")
                .cellPhone("01011112222")
                .birth(LocalDate.now())
                .profileImage("default.jpg")
                .name("꽃감이")
                .build();
    }

    private Password createPassword(String inputPassword) {
        byte[] salt = passwordEncoder.getSalt();
        String encryptedPassword = passwordEncoder.encrypt(inputPassword, salt);

        return Password.builder()
                .password(encryptedPassword)
                .salt(salt)
                .build();
    }

}