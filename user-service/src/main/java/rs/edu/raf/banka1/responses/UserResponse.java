package rs.edu.raf.banka1.responses;

import lombok.Getter;
import lombok.Setter;
import rs.edu.raf.banka1.dtos.PermissionDto;

import java.util.List;

@Getter
@Setter
public class UserResponse {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String position;
    private String phoneNumber;
    private Boolean active;
    private List<PermissionDto> permissions;
}

