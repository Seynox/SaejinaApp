package fr.seynox.saejinaapp.configuration;

import fr.seynox.saejinaapp.listeners.TicketEventsListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.security.auth.login.LoginException;

@Configuration
public class JDAConfiguration {

    @Value("${discord.token}")
    private String token;

    @Bean
    public JDA getJDA(TicketEventsListener ticketEventsListener) throws LoginException {
        return JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MEMBERS)
                .setEventManager(new AnnotatedEventManager())
                .addEventListeners(ticketEventsListener)
                .build();
    }

}
