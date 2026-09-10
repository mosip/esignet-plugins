package org.mock.esignet.plugin.repositories;

import org.mock.esignet.plugin.dto.UserDetail;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
@Repository
public class UserDetailRepository {
    private static final String QUERY = "select * from user_detail where id=?";

    @Autowired
    @Qualifier("mockPluginJdbcTemplate")
    private JdbcTemplate mockPluginJdbcTemplate;

    public UserDetail findUserById(String individualId) {
        List<UserDetail> list = mockPluginJdbcTemplate.query(QUERY, new Object[]{individualId}, new RowMapper<UserDetail>() {
            @Override
            public UserDetail mapRow(ResultSet resultSet, int i) throws SQLException {
                return new UserDetail(resultSet.getString(1),resultSet.getString(2), resultSet.getString(3), resultSet.getString(4));
            }
        });

        if(!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }
}
