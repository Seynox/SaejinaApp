package fr.seynox.saejinaapp.configuration;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.security.auth.login.LoginException;

@Configuration
public class JDAConfiguration {

    @Value("${discord.token}")
    private String token;

    @Bean
    public JDA getJDA() throws LoginException {
        return JDABuilder.createDefault(token)
                .setEventManager(new AnnotatedEventManager())
                .build();
    }

}
