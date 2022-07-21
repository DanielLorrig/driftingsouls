DELIMITER //
CREATE PROCEDURE get_enemy_ships_in_system(
	IN userid Int,
    IN star_system Int
)
BEGIN
	SELECT s.star_system, s.x, s.y, s.owner, ur.status, n.type nebeltype, MAX(COALESCE(sm.size, st.size)) max_size
	FROM ships s
			 LEFT JOIN (SELECT ur.target_id, ur.status FROM user_relations ur WHERE ur.user_id=userid AND ur.status<>2) ur ON s.owner = ur.target_id
			 INNER JOIN ship_types st
						ON st.id = s.type
			 LEFT JOIN nebel n
					   ON (n.star_system = s.star_system AND n.x = s.x AND n.y = s.y)
				LEFT JOIN ships_modules sm
					ON sm.id = s.modules
	WHERE
		s.owner<>-1 AND
		COALESCE(n.type, 1) NOT IN (4, 5, 6) AND
		(ur.target_id is null OR ur.status<>2)
        AND s.star_system = star_system

	GROUP BY s.star_system, s.x, s.y, s.owner, ur.status;
END;
//