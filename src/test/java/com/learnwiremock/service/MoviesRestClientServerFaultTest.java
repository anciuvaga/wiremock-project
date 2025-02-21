package com.learnwiremock.service;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.learnwiremock.constants.MoviesAppConstants;

import com.learnwiremock.exception.MovieErrorResponse;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;


import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(WireMockExtension.class)
public class MoviesRestClientServerFaultTest {

    MoviesRestClient moviesRestClient;
    WebClient webClient;

    TcpClient tcpClient = TcpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .doOnConnected( connection -> {
                connection.addHandlerLast(new ReadTimeoutHandler(5)).addHandlerLast(new WriteTimeoutHandler(5));
            });


    @InjectServer
    WireMockServer wireMockServer;

    @ConfigureWireMock
    Options options = wireMockConfig().port(8088)
            .notifier(new ConsoleNotifier(true))
            .extensions(new ResponseTemplateTransformer(true));

    @BeforeEach
    void setUp() {
        int port = wireMockServer.port();
        String baseUrl = String.format("http://localhost:%s", port);
//        webClient = WebClient.create(baseUrl);
        webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient)))
                .baseUrl(baseUrl).build();
        moviesRestClient = new MoviesRestClient(webClient);
    }

    @Test
    void retrieveAllMovies() {
        stubFor(get(urlPathEqualTo(MoviesAppConstants.GET_ALL_MOVIES_V1))
                .willReturn(serverError()));

        assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retriveAllMovies());
    }

    @Test
    void retrieveAllMovies_503_serviceUnavailable() {
        stubFor(get(urlPathEqualTo(MoviesAppConstants.GET_ALL_MOVIES_V1))
                .willReturn(serverError().withStatus(HttpStatus.SERVICE_UNAVAILABLE.value()).withBody("Service Unavailable")));

       MovieErrorResponse movieErrorResponse = assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retriveAllMovies());
       assertEquals("Service Unavailable", movieErrorResponse.getMessage());
    }

    @Test
    void retrieveAllMoviesFaultResponse() {
        stubFor(get(urlPathEqualTo(MoviesAppConstants.GET_ALL_MOVIES_V1))
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        MovieErrorResponse movieErrorResponse = assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retriveAllMovies());
        String errorResponse = "Connection prematurely closed BEFORE response";
        assertTrue(movieErrorResponse.getMessage().contains(errorResponse));
    }

    @Test
    void retrieveAllMovies_with_Fixed_Delay() {
        stubFor(get(urlPathEqualTo(MoviesAppConstants.GET_ALL_MOVIES_V1))
                .willReturn(ok().withFixedDelay(10000)));

        assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retriveAllMovies());
    }

    @Test
    void retrieveAllMovies_with_Random_Delay() {
        stubFor(get(urlPathEqualTo(MoviesAppConstants.GET_ALL_MOVIES_V1))
                .willReturn(ok().withUniformRandomDelay(6000, 10000)));

        assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retriveAllMovies());
    }

}
