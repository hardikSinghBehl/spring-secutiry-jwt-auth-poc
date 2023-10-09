package com.behl.cerberus.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.behl.cerberus.dto.UserCreationRequestDto;
import com.behl.cerberus.dto.UserDetailDto;
import com.behl.cerberus.dto.UserUpdationRequestDto;
import com.behl.cerberus.service.UserService;
import com.behl.cerberus.utility.AuthenticatedUserIdProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/users")
public class UserController {

	private final UserService userService;
	private final AuthenticatedUserIdProvider authenticatedUserIdProvider;

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Creates a user account", description = "Registers a unique user record in the system corresponding to the provided information")
	@ApiResponses(value = { 
			@ApiResponse(responseCode = "201", description = "User account created successfully"),
			@ApiResponse(responseCode = "409", description = "User account with provided email-id already exists") })
	public ResponseEntity<HttpStatus> createUser(@Valid @RequestBody final UserCreationRequestDto userCreationRequest) {
		userService.create(userCreationRequest);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Updates user account details", description = "Updates account details for the logged-in user")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User account details updated successfully") })
	@PreAuthorize("hasAnyAuthority('selfservice.write', 'fullaccess')")
	public ResponseEntity<HttpStatus> updateUser(@Valid @RequestBody final UserUpdationRequestDto userUpdationRequest) {
		final var userId = authenticatedUserIdProvider.getUserId();
		userService.update(userId, userUpdationRequest);
		return ResponseEntity.status(HttpStatus.OK).build();
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Retrieves current logged-in user's account details", description = "Private endpoint which retreives user account details against the Access-token JWT provided in headers")
	@PreAuthorize("hasAnyAuthority('selfservice.read', 'fullaccess')")
	public ResponseEntity<UserDetailDto> retrieveUser() {
		final var userId = authenticatedUserIdProvider.getUserId();
		final var userDetail = userService.getById(userId);
		return ResponseEntity.ok(userDetail);
	}

}
