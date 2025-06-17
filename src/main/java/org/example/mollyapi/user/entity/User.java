package org.example.mollyapi.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.mollyapi.common.entity.Base;
import org.example.mollyapi.common.exception.CustomException;
import org.example.mollyapi.common.exception.error.impl.OrderError;
import org.example.mollyapi.common.exception.error.impl.UserError;
import org.example.mollyapi.user.dto.UpdateUserReqDto;
import org.example.mollyapi.user.type.Sex;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "users")
public class User extends Base {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false)
    private String nickname;

    @Column(length = 11, nullable = false)
    private String cellPhone;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Sex sex;

    @Column(nullable = false)
    private Boolean flag;

    private String profileImage;

    private LocalDate birth;

    private Integer point;

    private String name;

    @Builder
    public User(String nickname, String cellPhone, Sex sex, String profileImage, LocalDate birth, String name) {
        this.nickname = nickname;
        this.cellPhone = cellPhone;
        this.sex = sex;
        this.flag = false;
        this.profileImage = profileImage;
        this.birth = birth;
        this.point = 0;
        this.name = name;
    }

    public boolean updateUser(UpdateUserReqDto updateUserReqDto){
        boolean isUpdate = false;

        String updateName = updateUserReqDto.name();
        if( updateName != null && !updateName.isBlank() && !this.name.equals(updateName)){
            this.name = updateName;
            isUpdate = true;
        }

        String updatedCellPhone = updateUserReqDto.cellPhone();
        if( updatedCellPhone != null && !updatedCellPhone.isBlank() && !this.cellPhone.equals(updatedCellPhone)){
            this.cellPhone = updatedCellPhone;
            isUpdate = true;
        }

        if(!this.birth.isEqual(updateUserReqDto.birth())){
            this.birth = updateUserReqDto.birth();
            isUpdate = true;
        }

        String updatedNicname = updateUserReqDto.nickname();
        if (nickname != null && !updatedNicname.isBlank() && !this.nickname.equals(updatedNicname)){
            this.nickname = updatedNicname;
            isUpdate = true;
        }

        return isUpdate;
    }

    public void updateFlag(){
        this.flag = true;
    }

    public void updatePoint(int amount) {
        if (this.point == null) {
            this.point = 0;
        }
        this.point += amount;
    }

    public void deductPoint(int amount) {
        if (this.point - amount < 0){
            throw new CustomException(OrderError.INSUFFICIENT_POINT);
        } else{
            this.point -= amount;
        };
    }

}
