package io.github.hyinorbitcode.couponlab.user;

public record CurrentUser(long id, Role role) {

    public boolean isAdmin(){
        return role == Role.ADMIN;
    }
    
}
