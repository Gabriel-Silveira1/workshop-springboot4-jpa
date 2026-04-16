package com.coursejava.coursejava.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coursejava.coursejava.entities.Product;

public interface ProductRepository extends JpaRepository<Product, Long>{
	
}
