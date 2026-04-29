package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Facility;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

	boolean existsByIdAndUsers_Id(Long facilityId, Long userId);

	@Query("""
		select distinct f
		from Facility f
		left join f.users u
		where (:isAdmin = true or u.id = :userId)
		  and (:includeInactive = true or f.isActive = true)
		order by f.name asc
		""")
	List<Facility> findAccessibleFacilities(
		@Param("userId") Long userId,
		@Param("isAdmin") boolean isAdmin,
		@Param("includeInactive") boolean includeInactive
	);
}