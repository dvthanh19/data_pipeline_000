db.auth('mongo', '1234')
db = db.getSiblingDB('mydb')

db.createUser(
    {
        user: "testuser",
        pwd: "1234",
        roles: [
            {
                role: "readWrite",
                db: "mydb"
            }
        ]
    }
);