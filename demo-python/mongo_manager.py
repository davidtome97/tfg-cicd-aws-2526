from pymongo import MongoClient
from config import MONGO_URI, DB_NAME

class MongoDBManager:
    def __init__(self):
        self.client = None
        self.db = None

    def connect(self):
        if self.client is None:
            self.client = MongoClient(MONGO_URI)
            # Si la URI no define DB por defecto, usamos DB_NAME
            default_db = self.client.get_default_database()
            self.db = default_db if default_db is not None else self.client[DB_NAME]
        return self.db

    def get_collection(self, name):
        db = self.connect()
        return db[name]

mongo_manager = MongoDBManager()