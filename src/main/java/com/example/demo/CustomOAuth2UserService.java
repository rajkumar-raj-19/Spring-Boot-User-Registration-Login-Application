package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);
        
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        // Google uses the key "picture" for the profile image URL
        String picture = oauthUser.getAttribute("picture"); 

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            User newUser = new User();
            newUser.setFullName(name);
            newUser.setEmail(email);
            newUser.setPhotoUrl(picture); // Saving Google picture URL here
            newUser.setProvider("GOOGLE");
            userRepository.save(newUser);
        } else {
            // Optional: Update the photo if it changed
            User existingUser = userOptional.get();
            existingUser.setPhotoUrl(picture);
            userRepository.save(existingUser);
        }

        return oauthUser;
    }
}