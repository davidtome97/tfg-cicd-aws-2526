from pymongo import MongoClient
from config import MONGO_URI

class MongoDBManager:
    def __init__(self):
        self.client = MongoClient(MONGO_URI)
        self.db = self.client.get_database()  # usa la BD de la URI

    def get_collection(self, name):
        return self.db[name]

mongo_manager = MongoDBManager()