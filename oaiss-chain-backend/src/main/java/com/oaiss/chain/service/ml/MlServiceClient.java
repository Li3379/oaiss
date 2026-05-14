package com.oaiss.chain.service.ml;

import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.constant.ErrorMessage;
import com.oaiss.chain.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Generic HTTP client for calling ML service endpoints.
 *
 * <p>Provides a type-safe {@code post()} method with error handling for
 * connection failures, timeouts, and non-2xx responses from the ML service.</p>
 *
 * @author OAISS Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MlServiceClient {

    private final WebClient mlWebClient;

    /**
     * Send a POST request to the ML service.
     *
     * @param endpoint    relative path (e.g. "/api/v1/predict/emission")
     * @param request     request body (serialised to JSON)
     * @param responseType expected response type
     * @param <T>         response type parameter
     * @return deserialized response from ML service
     * @throws BusinessException when ML service is unavailable or returns an error
     */
    public <T> T post(String endpoint, Object request, Class<T> responseType) {
        return mlWebClient.post()
                .uri(endpoint)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(responseType)
                .onErrorMap(WebClientRequestException.class, ex -> {
                    log.error("ML service connection failed for {}: {}", endpoint, ex.getMessage());
                    return new BusinessException(
                            ErrorCode.ML_SERVICE_UNAVAILABLE,
                            ErrorMessage.ML_SERVICE_UNAVAILABLE);
                })
                .onErrorMap(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE
                            || ex.getStatusCode() == HttpStatus.NOT_IMPLEMENTED) {
                        log.warn("ML service returned {} for {}: {}",
                                ex.getStatusCode(), endpoint, ex.getResponseBodyAsString());
                        return new BusinessException(
                                ErrorCode.ML_SERVICE_UNAVAILABLE,
                                ErrorMessage.ML_SERVICE_UNAVAILABLE);
                    }
                    log.error("ML service returned {} for {}: {}",
                            ex.getStatusCode(), endpoint, ex.getResponseBodyAsString());
                    return new BusinessException(
                            ErrorCode.ML_SERVICE_ERROR,
                            ErrorMessage.ML_SERVICE_ERROR);
                })
                .block();
    }
}