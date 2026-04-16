package com.coursejava.coursejava.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coursejava.coursejava.entities.User;

public interface UserRepository extends JpaRepository<User, Long>{
	
}
