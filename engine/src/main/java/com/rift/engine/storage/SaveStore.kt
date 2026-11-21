package com.rift.engine.storage
/** Platform-neutral key/value save contract. */
interface SaveStore {
 fun putString(key:String,value:String)
 fun getString(key:String,defaultValue:String=""):String
 fun putFloat(key:String,value:Float)
 fun getFloat(key:String,defaultValue:Float=0f):Float
}
class InMemorySaveStore:SaveStore {
 private val data=mutableMapOf<String,Any>()
 @Synchronized override fun putString(key:String,value:String){data[key]=value}
 @Synchronized override fun getString(key:String,defaultValue:String)=data[key] as? String ?: defaultValue
 @Synchronized override fun putFloat(key:String,value:Float){data[key]=value}
 @Synchronized override fun getFloat(key:String,defaultValue:Float)=data[key] as? Float ?: defaultValue
}
