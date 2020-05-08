package com.example.ladm_u3_practica2_jonathanlopez

import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.actualizar_pedido.*
import kotlinx.android.synthetic.main.nuevo_pedido.*
import kotlinx.android.synthetic.main.nuevo_pedido.view.*

class MainActivity : AppCompatActivity() {
    var baseRemota = FirebaseFirestore.getInstance()
    var dataLista = ArrayList<String>()
    var listaID = ArrayList<String>()

    /*
    nombre (cadena)
    domicilio (cadena)
    celular (cadena)
    descripcion / producto (cadena)
    precio (decimal)
    cantidad (entero)
    entregado (booleano)*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnConsultar.setOnClickListener {
            realizarConsulta(txtValor.text.toString(),cmbFiltro.selectedItemPosition)
        }

        lista.setOnItemClickListener { parent, view, position, id ->
            if(listaID.size==0){
                return@setOnItemClickListener
            }
            AlertaEliminarActualizar(position)
        }
        baseRemota.collection("restaurante")
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException!=null){
                    Toast.makeText(this,"Error no se puede acceder a consulta",Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                dataLista.clear()
                listaID.clear()
                for (document in querySnapshot!!){
                    var ajustePrecio =  document.getDouble("pedido.precio").toString()
                    if (ajustePrecio.indexOf(".")+2 == ajustePrecio.length) {
                       ajustePrecio=ajustePrecio
                    }else{ajustePrecio = ajustePrecio.substring(0, ajustePrecio.indexOf(".") + 3)}

                    var cad="\n-CLIENTE-\n"+
                            "       Nombre: "+ document.getString("nombre")+"\n"+
                            "       Domicilio: "+ document.getString("domicilio")+"\n"+
                            "       Celular: "+ document.getString("celular")+"\n"+
                            "    -PRODUCTO-\n"+
                            "        Producto: "+ document.getString("pedido.producto")+"\n"+
                            "        Precio: $"+ ajustePrecio+"\n"+
                            "        Cantidad: "+ document.get("pedido.cantidad")+"\n"+
                            "        Entregado: "+ validarEstatus( document.getBoolean("pedido.entregado"))+"\n"
                    dataLista.add(cad)
                    listaID.add(document.id)
                }
                if(dataLista.size==0){
                    dataLista.add("No hay nada")
                }
                var adaptador = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataLista)
                lista.adapter =adaptador

            }

        btnNuevo.setOnClickListener {
            construirDialogoNuevoPedido()
        }
    }

    private fun construirDialogoNuevoPedido() {
        var dialogo = Dialog(this)
        dialogo.setContentView(R.layout.nuevo_pedido)

       var nombre=  dialogo.findViewById<EditText>(R.id.txtNombre)
       var domicilio=  dialogo.findViewById<EditText>(R.id.txtDomicilio)
       var celular=  dialogo.findViewById<EditText>(R.id.txtCelular)
       var descripcion=  dialogo.findViewById<EditText>(R.id.txtDescripcion)
       var precio=  dialogo.findViewById<EditText>(R.id.txtPrecio)
       var cantidad=  dialogo.findViewById<EditText>(R.id.txtCantidad)
       var entregado=  dialogo.findViewById<RadioButton>(R.id.checkEntregado)
       var n_Entregado=  dialogo.findViewById<RadioButton>(R.id.checkNoEntregado)
       var agregar=  dialogo.findViewById<Button>(R.id.btnAgregar)
       var cancelar=  dialogo.findViewById<Button>(R.id.btnCancelar)


        //Boton agregar
        agregar.setOnClickListener {
            var data = hashMapOf(
                "nombre" to nombre.text.toString(),
                "domicilio" to domicilio.text.toString(),
                "celular" to celular.text.toString()
            )

            baseRemota.collection("restaurante")
                .add(data)
                .addOnSuccessListener {

                    var estatusPedido : Boolean = true
                    if(entregado.isChecked){estatusPedido=true}
                    if(n_Entregado.isChecked){estatusPedido=false}

                    var pedido = hashMapOf(
                        "producto" to descripcion.text.toString(),
                        "precio" to precio.text.toString().toFloat(),
                        "cantidad" to cantidad.text.toString().toInt(),
                        "entregado" to estatusPedido
                    )
                    baseRemota.collection("restaurante")
                        .document(it.id)
                        .update("pedido",pedido as Map<String,Any>)

                        Toast.makeText(this,"Se capturo nuevo pedido", Toast.LENGTH_LONG).show()
                        //Limpiar Campos
                        nombre.setText("")
                        domicilio.setText("")
                        celular.setText("")
                        descripcion.setText("")
                        precio.setText("")
                        cantidad.setText("")
                        entregado.setText("")
                    }.addOnCanceledListener {
                    Toast.makeText(this,"Error: No se capturó el pedido, intente de nuevo",Toast.LENGTH_LONG).show()
                     }
                    dialogo.dismiss()
        }
        //Boton cancelar
        cancelar.setOnClickListener {dialogo.dismiss() }

        dialogo.show()
    }

    private fun construirDialogoNuevoActualizarPedido(ID:String) {
        var dialogo = Dialog(this)
        var cambEstado = Dialog(this)
        dialogo.setContentView(R.layout.actualizar_pedido)
        cambEstado.setContentView(R.layout.cambiar_estado)

        var nombre=  dialogo.findViewById<EditText>(R.id.txtNombre2U)
        var domicilio=  dialogo.findViewById<EditText>(R.id.txtDomicilio2U)
        var celular=  dialogo.findViewById<EditText>(R.id.txtCelular2U)
        var descripcion=  dialogo.findViewById<EditText>(R.id.txtDescripcion2U)
        var precio=  dialogo.findViewById<EditText>(R.id.txtPrecio2U)
        var cantidad=  dialogo.findViewById<EditText>(R.id.txtCantidad2U)
        var txtestatus=  dialogo.findViewById<TextView>(R.id.txtEstatus)
        var cambiarEstado = dialogo.findViewById<Button>(R.id.btnCambiarEstado)
        var actualizar=  dialogo.findViewById<Button>(R.id.btnAgregar2U)
        var cancelar=  dialogo.findViewById<Button>(R.id.btnCancelar2U)


        cambiarEstado.setOnClickListener {
            var checkE = cambEstado.findViewById<RadioButton>(R.id.entregadoCheck)
            var checkNE = cambEstado.findViewById<RadioButton>(R.id.noEntregadoCheck)
            var acepCambio=  cambEstado.findViewById<Button>(R.id.btnAceptarCambio)
            var recCambio=  cambEstado.findViewById<Button>(R.id.btnCancelarCambio)
            acepCambio.setOnClickListener {
                if(checkE.isChecked){txtestatus.setText("Entregado")}
                if(checkNE.isChecked){txtestatus.setText("No entregado")}
                cambEstado.dismiss()
            }
            recCambio.setOnClickListener {
                cambEstado.dismiss()
            }


            cambEstado.show()
        }
        //Poner datos
        baseRemota.collection("restaurante")
            .document(ID)
            .get()
            .addOnSuccessListener {
                var ajustePrecio =  it.getDouble("pedido.precio").toString()

                if (ajustePrecio.indexOf(".")+2 == ajustePrecio.length) {
                    ajustePrecio=ajustePrecio
                }else{ajustePrecio = ajustePrecio.substring(0, ajustePrecio.indexOf(".") + 3)}

                       nombre.setText( it.getString("nombre"))
                       domicilio.setText( it.getString("domicilio"))
                       celular.setText( it.getString("celular"))
                       descripcion.setText( it.getString("pedido.producto"))
                       precio.setText( ajustePrecio)
                       cantidad.setText( it.get("pedido.cantidad").toString())
                       txtestatus.setText(validarEstatus(it.getBoolean("pedido.entregado")))
            }
            .addOnFailureListener {
                Toast.makeText(this,"Error: No hay conexión de red.",Toast.LENGTH_LONG).show()
            }

        //Boton
        actualizar.setOnClickListener {
            baseRemota.collection("restaurante")
                .document(ID)
                .update(
                                  "nombre",nombre.text.toString(),
                    "domicilio",domicilio.text.toString(),
                                        "celular",celular.text.toString(),
                                        "pedido.producto",descripcion.text.toString(),
                                        "pedido.cantidad",cantidad.text.toString().toInt(),
                                        "pedido.precio",precio.text.toString().toDouble(),
                                        "pedido.entregado",validarEstatusInverso(txtestatus.text.toString())
                )
                .addOnSuccessListener {
                    Toast.makeText(this,"Actualizaión realizada",Toast.LENGTH_LONG).show()
                    dialogo.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(this,"No se pudo actualizar", Toast.LENGTH_LONG).show()
                }
        }
        //Boton cancelar
        cancelar.setOnClickListener {dialogo.dismiss() }
        dialogo.show()
    }
    fun validarEstatus (b : Boolean?) :String{
        if(b==true){return "Entregado"}
        else{return "No entregado"}
    }
    fun validarEstatusInverso (s : String) :Boolean{
        if(s.equals("Entregado")){return true}
        if(s.equals("No entregado")){return false}
        return true
    }

    private fun AlertaEliminarActualizar(position: Int) {
        AlertDialog.Builder(this).setTitle("Atencion")
            .setMessage("¿Qué desea hacer con:  ${dataLista[position]}")
            .setPositiveButton("Eliminar"){d,i->

                eliminar(listaID[position])
            }
            .setNegativeButton("Actualizar"){d,i->
            construirDialogoNuevoActualizarPedido(listaID[position])
            }
            .setNeutralButton("Cancelar"){d,i->}
            .show()
    }
    private fun eliminar(idEliminar: String) {
        baseRemota.collection("restaurante")
            .document(idEliminar).delete() //se posiciona sobre un documento
            .addOnSuccessListener {
                Toast.makeText(this,"Se eliminó con éxito",Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener{
                Toast.makeText(this,"Ocurrió un error a la hora de elimnar",Toast.LENGTH_LONG).show()
            }
    }
    private fun realizarConsulta(valor:String,clave:Int) {
        /*
        * Clave:
        *       0 Todos
        *       1 Nombre
                2 Domicilio
                3 Celular
                4 Descripcion
                5 Precio
                6 Cantidad
                7 Estatus
        */
        when (clave) {

            0 -> {
                consultaTodos()
            }

            1 -> {
                consultaNombre(txtValor.text.toString())
            }

            2 -> {
                consultaDomicilio(txtValor.text.toString())
            }

            3 -> {
                consultaCelular(txtValor.text.toString())
            }

            4 -> {
                consultaDescripcion(txtValor.text.toString())
            }

            5 -> {
                consultaPrecio(txtValor.text.toString().toFloat())
            }

            6 -> {
                consultaCantidad(txtValor.text.toString().toInt())
            }

            7 -> {
                consultaEstatus(true)
            }
            8 -> {
                consultaEstatus(false)
            }
        }
    }

    private fun consultaEstatus( valor: Boolean) {
        baseRemota.collection("restaurante")
            .whereEqualTo("pedido.entregado",valor)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException!=null){
                    Toast.makeText(this,"Error no se puede acceder a consulta",Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                dataLista.clear()
                listaID.clear()
                for (document in querySnapshot!!){
                    var ajustePrecio =  document.getDouble("pedido.precio").toString()
                    if (ajustePrecio.indexOf(".")+2 == ajustePrecio.length) {
                        ajustePrecio=ajustePrecio
                    }else{ajustePrecio = ajustePrecio.substring(0, ajustePrecio.indexOf(".") + 3)}

                    var cad="+\n Precio: $"+ ajustePrecio+"\n\n"+
                            "   -CLIENTE-\n"+
                            "      Nombre: "+ document.getString("nombre")+"\n"+
                            "      Domicilio: "+ document.getString("domicilio")+"\n"+
                            "      Celular: "+ document.getString("celular")+"\n"+
                            "    -PRODUCTO-\n"+
                            "        Producto: "+ document.getString("pedido.producto")+"\n"+
                            "        Cantidad: "+ document.get("pedido.cantidad")+"\n"+
                            "        Entregado: "+ validarEstatus( document.getBoolean("pedido.entregado"))+"\n"
                    dataLista.add(cad)
                    listaID.add(document.id)
                }
                if(dataLista.size==0){
                    dataLista.add("Sin resultados de busqueda")
                }
                var adaptador = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataLista)
                lista.adapter =adaptador

            }

    }

    private fun consultaCantidad(valor : Int) {
        baseRemota.collection("restaurante")
            .whereGreaterThanOrEqualTo("pedido.cantidad",valor)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException!=null){
                    Toast.makeText(this,"Error no se puede acceder a consulta",Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                dataLista.clear()
                listaID.clear()
                for (document in querySnapshot!!){
                    var ajustePrecio =  document.getDouble("pedido.precio").toString()
                    if (ajustePrecio.indexOf(".")+2 == ajustePrecio.length) {
                        ajustePrecio=ajustePrecio
                    }else{ajustePrecio = ajustePrecio.substring(0, ajustePrecio.indexOf(".") + 3)}

                    var cad="\n Cantidad: "+ document.get("pedido.cantidad")+"\n\n"+
                            "    -CLIENTE-\n"+
                            "      Nombre: "+ document.getString("nombre")+"\n"+
                            "      Domicilio: "+ document.getString("domicilio")+"\n"+
                            "      Celular: "+ document.getString("celular")+"\n"+
                            "    -PRODUCTO-\n"+
                            "        Producto: "+ document.getString("pedido.producto")+"\n"+
                            "        Precio: $"+ ajustePrecio+"\n"+
                            "        Entregado: "+ validarEstatus( document.getBoolean("pedido.entregado"))+"\n"
                    dataLista.add(cad)
                    listaID.add(document.id)
                }
                if(dataLista.size==0){
                    dataLista.add("Sin resultados de busqueda")
                }
                var adaptador = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataLista)
                lista.adapter =adaptador

            }
    }

    private fun consultaPrecio(valor:Float) {
        baseRemota.collection("restaurante")
            .whereGreaterThanOrEqualTo("pedido.precio",valor.toDouble())
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException!=null){
                    Toast.makeText(this,"Error no se puede acceder a consulta",Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                dataLista.clear()
                listaID.clear()
                for (document in querySnapshot!!){
                    var ajustePrecio =  document.getDouble("pedido.precio").toString()
                    if (ajustePrecio.indexOf(".")+2 == ajustePrecio.length) {
                        ajustePrecio=ajustePrecio
                    }else{ajustePrecio = ajustePrecio.substring(0, ajustePrecio.indexOf(".") + 3)}

                    var cad="\n Precio: $"+ ajustePrecio+"\n\n"+
                            "   -CLIENTE-\n"+
                            "      Nombre: "+ document.getString("nombre")+"\n"+
                            "      Domicilio: "+ document.getString("domicilio")+"\n"+
                            "      Celular: "+ document.getString("celular")+"\n"+
                            "    -PRODUCTO-\n"+
                            "        Producto: "+ document.getString("pedido.producto")+"\n"+
                            "        Cantidad: "+ document.get("pedido.cantidad")+"\n"+
                            "        Entregado: "+ validarEstatus( document.getBoolean("pedido.entregado"))+"\n"
                    dataLista.add(cad)
                    listaID.add(document.id)
                }
                if(dataLista.size==0){
                    dataLista.add("Sin resultados de busqueda")
                }
                var adaptador = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataLista)
                lista.adapter =adaptador

            }
    }

    private fun consultaDescripcion(valor : String) {
        baseRemota.collection("restaurante")
            .whereEqualTo("pedido.producto",valor)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException!=null){
                    Toast.makeText(this,"Error no se puede acceder a consulta",Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                dataLista.clear()
                listaID.clear()
                for (document in querySnapshot!!){
                    var ajustePrecio =  document.getDouble("pedido.precio").toString()
                    if (ajustePrecio.indexOf(".")+2 == ajustePrecio.length) {
                        ajustePrecio=ajustePrecio
                    }else{ajustePrecio = ajustePrecio.substring(0, ajustePrecio.indexOf(".") + 3)}

                    var cad="\n Producto: "+ document.getString("pedido.producto")+"\n\n"+
                            "   -CLIENTE-\n"+
                            "      Nombre: "+ document.getString("nombre")+"\n"+
                            "      Domicilio: "+ document.getString("domicilio")+"\n"+
                            "      Celular: "+ document.getString("celular")+"\n"+
                            "    -PRODUCTO-\n"+
                            "        Precio: $"+ ajustePrecio+"\n"+
                            "        Cantidad: "+ document.get("pedido.cantidad")+"\n"+
                            "        Entregado: "+ validarEstatus( document.getBoolean("pedido.entregado"))+"\n"
                    dataLista.add(cad)
                    listaID.add(document.id)
                }
                if(dataLista.size==0){
                    dataLista.add("Sin resultados de busqueda")
                }
                var adaptador = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataLista)
                lista.adapter =adaptador

            }
    }

    private fun consultaCelular(valor : String) {
        baseRemota.collection("restaurante")
            .whereEqualTo("celular", valor)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Toast.makeText(this, "Error no se puede acceder a consulta", Toast.LENGTH_LONG)
                        .show()
                    return@addSnapshotListener
                }
                dataLista.clear()
                listaID.clear()
                for (document in querySnapshot!!) {
                    var ajustePrecio = document.getDouble("pedido.precio").toString()
                    if (ajustePrecio.indexOf(".") + 2 == ajustePrecio.length) {
                        ajustePrecio = ajustePrecio
                    } else {
                        ajustePrecio = ajustePrecio.substring(0, ajustePrecio.indexOf(".") + 3)
                    }

                    var cad ="\n Celular: " + document.getString("celular") + "\n\n" +

                            "   -CLIENTE-\n" +
                            "      Nombre: " + document.getString("nombre") + "\n" +
                            "      Domicilio: " + document.getString("domicilio") + "\n" +
                            "    -PRODUCTO-\n" +
                            "        Producto: " + document.getString("pedido.producto") + "\n" +
                            "        Precio: $" + ajustePrecio + "\n" +
                            "        Cantidad: " + document.get("pedido.cantidad") + "\n" +
                            "        Entregado: " + validarEstatus(document.getBoolean("pedido.entregado"))+"\n"
                    dataLista.add(cad)
                    listaID.add(document.id)
                }
                if (dataLista.size == 0) {
                    dataLista.add("NSin resultados de busqueda")
                }
                var adaptador =
                    ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataLista)
                lista.adapter = adaptador

            }
    }

    private fun consultaDomicilio(valor : String) {
        baseRemota.collection("restaurante")
            .whereEqualTo("domicilio", valor)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Toast.makeText(this, "Error no se puede acceder a consulta", Toast.LENGTH_LONG)
                        .show()
                    return@addSnapshotListener
                }
                dataLista.clear()
                listaID.clear()
                for (document in querySnapshot!!) {
                    var ajustePrecio = document.getDouble("pedido.precio").toString()
                    if (ajustePrecio.indexOf(".") + 2 == ajustePrecio.length) {
                        ajustePrecio = ajustePrecio
                    } else {
                        ajustePrecio = ajustePrecio.substring(0, ajustePrecio.indexOf(".") + 3)
                    }

                    var cad =
                            "\n Domicilio: " + document.getString("domicilio") + "\n\n" +
                            "   -CLIENTE-\n" +
                            "      Nombre: " + document.getString("nombre") + "\n" +
                            "      Celular: " + document.getString("celular") + "\n" +
                            "      Domicilio: " + document.getString("domicilio") + "\n" +
                            "    -PRODUCTO-\n" +
                            "        Producto: " + document.getString("pedido.producto") + "\n" +
                            "        Precio: $" + ajustePrecio + "\n" +
                            "        Cantidad: " + document.get("pedido.cantidad") + "\n" +
                            "        Entregado: " + validarEstatus(document.getBoolean("pedido.entregado"))+"\n"
                    dataLista.add(cad)
                    listaID.add(document.id)
                }
                if (dataLista.size == 0) {
                    dataLista.add("Sin resultados de busqueda")
                }
                var adaptador =
                    ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataLista)
                lista.adapter = adaptador

            }
    }

    private fun consultaNombre(valor :String) {
        baseRemota.collection("restaurante")
            .whereEqualTo("nombre", valor)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    Toast.makeText(this, "Error no se puede acceder a consulta", Toast.LENGTH_LONG)
                        .show()
                    return@addSnapshotListener
                }
                dataLista.clear()
                listaID.clear()
                for (document in querySnapshot!!) {
                    var ajustePrecio = document.getDouble("pedido.precio").toString()
                    if (ajustePrecio.indexOf(".") + 2 == ajustePrecio.length) {
                        ajustePrecio = ajustePrecio
                    } else {
                        ajustePrecio = ajustePrecio.substring(0, ajustePrecio.indexOf(".") + 3)
                    }

                    var cad =
                                "\n Nombre: " + document.getString("nombre") + "\n\n" +
                                "   -CLIENTE-\n" +
                                "      Nombre: " + document.getString("nombre") + "\n" +
                                "      Celular: " + document.getString("celular") + "\n" +
                                "      Domicilio: " + document.getString("domicilio") + "\n" +
                                "    -PRODUCTO-\n" +
                                "        Producto: " + document.getString("pedido.producto") + "\n" +
                                "        Precio: $" + ajustePrecio + "\n" +
                                "        Cantidad: " + document.get("pedido.cantidad") + "\n" +
                                "        Entregado: " + validarEstatus(document.getBoolean("pedido.entregado"))+"\n"
                    dataLista.add(cad)
                    listaID.add(document.id)
                }
                if (dataLista.size == 0) {
                    dataLista.add("Sin resultados de busqueda")
                }
                var adaptador =
                    ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataLista)
                lista.adapter = adaptador

            }
    }

    private fun consultaTodos() {
        baseRemota.collection("restaurante")
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(firebaseFirestoreException!=null){
                    Toast.makeText(this,"Error no se puede acceder a consulta",Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                dataLista.clear()
                listaID.clear()
                for (document in querySnapshot!!){
                    var ajustePrecio =  document.getDouble("pedido.precio").toString()
                    if (ajustePrecio.indexOf(".")+2 == ajustePrecio.length) {
                        ajustePrecio=ajustePrecio
                    }else{ajustePrecio = ajustePrecio.substring(0, ajustePrecio.indexOf(".") + 3)}

                    var cad="\n  -CLIENTE-\n"+
                            "       Nombre: "+ document.getString("nombre")+"\n"+
                            "       Domicilio: "+ document.getString("domicilio")+"\n"+
                            "       Celular: "+ document.getString("celular")+"\n"+
                            "    -PRODUCTO-\n"+
                            "        Producto: "+ document.getString("pedido.producto")+"\n"+
                            "        Precio: $"+ ajustePrecio+"\n"+
                            "        Cantidad: "+ document.get("pedido.cantidad")+"\n"+
                            "        Entregado: "+ validarEstatus( document.getBoolean("pedido.entregado"))+"\n"
                    dataLista.add(cad)
                    listaID.add(document.id)
                }
                if(dataLista.size==0){
                    dataLista.add("Sin resultados de busqueda")
                }
                var adaptador = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataLista)
                lista.adapter =adaptador

            }
    }

}
