import java.io.*;

import java.sql.*;

public class DatabaseInitializer {
    //检测编码用的函数
    public static String isUTF8(String filepath) {
        try (FileInputStream fis = new FileInputStream(filepath)) {
            while (true) {
                int curr = fis.read();
                if (curr == -1) {
                    return "UTF-8";
                }
                if (curr < 0x80) { // (10000000): 值小于0x80的为ASCII字符
                } else if (curr < 0xC0) { // (11000000): 值介于0x80与0xC0之间的为无效UTF-8字符
                    return "GBK";
                } else if (curr < 0xE0) { // (11100000): 此范围内为2字节UTF-8字符
                    if ((fis.read() & 0xC0) != 0x80) {
                        return "GBK";
                    }
                } else if (curr < 0xF0) { // (11110000): 此范围内为3字节UTF-8字符
                    if ((fis.read() & 0xC0) != 0x80 || (fis.read() & 0xC0) != 0x80) {
                        return "GBK";
                    }
                } else {
                    return "GBK";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "UTF-8";
        }
    }

    public void csv_to_database(String csv_filename) throws IOException {
        String path = "src/main/resources/"+csv_filename;
        String charsetName = isUTF8(path);
        //至此，编码类型就更新好了

        String dbUrl = "jdbc:mysql://localhost:3306/for_database_lab1?useUnicode=true&characterEncoding="+charsetName;//以这种编码类型写入
        String dbUser = "root";
        String dbPassword = "123456";
        String table_name = csv_filename.substring(0, csv_filename.length() - 4);

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            // 读取 CSV 文件
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), charsetName))) {
                // 读取 CSV 文件的第一行作为表头
                String headerLine = br.readLine();
                String[] columns = headerLine.split(",");

                // 检查是否存在同名表，如果存在就删除之前的表
                String checkTableExistenceSQL = "SHOW TABLES LIKE ?";
                try (PreparedStatement checkTableExistenceStatement = connection.prepareStatement(checkTableExistenceSQL)) {
                    checkTableExistenceStatement.setString(1, table_name);
                    try (ResultSet resultSet = checkTableExistenceStatement.executeQuery()) {
                        if (resultSet.next()) {
                            // 如果存在同名表，就删除之前的表
                            String dropTableSQL = "DROP TABLE " + table_name;
                            try (PreparedStatement dropTableStatement = connection.prepareStatement(dropTableSQL)) {
                                dropTableStatement.executeUpdate();
                            }
                        }
                    }
                }
                // 创建表
                String createTableSQL = "CREATE TABLE IF NOT EXISTS "+ table_name+"  (";
                for (String column : columns) {
                    createTableSQL += "`" + column + "` VARCHAR(255), ";//TODO:这里要加单引号，否则会报错：通常使用反引号（`）来引用表名和列名，而不是双引号。
                }
                createTableSQL = createTableSQL.substring(0, createTableSQL.length() - 2) + ")";
                try (PreparedStatement createTableStatement = connection.prepareStatement(createTableSQL)) {
                    createTableStatement.executeUpdate();
                }

                // 插入数据
                String insertSQL = "INSERT INTO "+ table_name + " VALUES (";
                for (int i = 0; i < columns.length; i++) {
                    insertSQL += "?, ";
                }
                insertSQL = insertSQL.substring(0, insertSQL.length() - 2) + ")";
                try (PreparedStatement insertStatement = connection.prepareStatement(insertSQL)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] data = line.split(",", -1);//TODO:这里要加-1，否则不能拿到最有一个空元素
                        for (int i = 0; i < data.length; i++) {
                            insertStatement.setString(i + 1, data[i]);
                        }
                        insertStatement.executeUpdate();
                    }
                }
            }
        } catch (SQLException | java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        DatabaseInitializer initializer = new DatabaseInitializer();
        initializer.csv_to_database("room.csv");
        initializer.csv_to_database("student.csv");
    }
}


