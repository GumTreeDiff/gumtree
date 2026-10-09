 /*
  * The MIT License
  *
  * Copyright 2012 Universidad de Montemorelos A. C.
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
  * THE SOFTWARE.
  */
 package mx.edu.um.mateo.inventario.model;
 
 import java.io.Serializable;
 import java.math.BigDecimal;
 import java.util.Objects;
 import javax.persistence.*;
 import org.hibernate.validator.constraints.NotBlank;
 
 /**
  *
  * @author J. David Mendoza <jdmendoza@um.edu.mx>
  */
 @Entity
 @Table(name = "productos", uniqueConstraints = {
     @UniqueConstraint(columnNames = {"almacen_id", "codigo"}),
     @UniqueConstraint(columnNames = {"almacen_id", "sku"}),
     @UniqueConstraint(columnNames = {"almacen_id", "nombre"})
 })
 public class Producto implements Serializable {
 
     @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;
     @Version
     private Integer version;
     @NotBlank
     @Column(nullable = false, length = 6)
     private String codigo;
     @NotBlank
     @Column(nullable = false, length = 64)
     private String sku;
     @NotBlank
     @Column(nullable = false, length = 128)
     private String nombre;
     @NotBlank
     @Column(nullable = false, length = 254)
     private String descripcion;
     @Column(length = 64)
     private String marca;
     @Column(length = 64)
     private String modelo;
     @NotBlank
     @Column(nullable = false, length = 32, name = "unidad_medida")
     private String unidadMedida = "Unidades";
     @Column(length = 128)
     private String ubicacion;
     @Column(nullable = false, scale = 2, precision = 8, name = "precio_unitario")
     private BigDecimal precioUnitario = new BigDecimal("0");
     @Column(nullable = false, scale = 2, precision = 8, name = "ultimo_precio")
     private BigDecimal ultimoPrecio = new BigDecimal("0");
     @Column(nullable = false, scale = 3, precision = 8)
     private BigDecimal existencia = new BigDecimal("0");
     @Column(nullable = false, scale = 3, precision = 8, name = "punto_reorden")
     private BigDecimal puntoReorden = new BigDecimal("0");
     @Column(nullable = false, scale = 2, precision = 8)
     private BigDecimal iva = new BigDecimal("0.16");
     @Column(nullable=false, name = "tiempo_entrega")
     private Integer tiempoEntrega = 3;
     @Column(nullable = false)
     private Boolean fraccion = false;
     @ManyToOne(optional = false)
     @JoinColumn(name = "tipo_producto_id")
     private TipoProducto tipoProducto;
     @ManyToOne(optional = false)
     private Almacen almacen;
 
     public Producto() {
     }
 
     public Producto(String codigo, String sku, String nombre, String descripcion, TipoProducto tipoProducto, Almacen almacen) {
         this.codigo = codigo;
         this.sku = sku;
         this.nombre = nombre;
         this.descripcion = descripcion;
         this.tipoProducto = tipoProducto;
         this.almacen = almacen;
     }
 
     /**
      * @return the id
      */
     public Long getId() {
         return id;
     }
 
     /**
      * @param id the id to set
      */
     public void setId(Long id) {
         this.id = id;
     }
 
     /**
      * @return the version
      */
     public Integer getVersion() {
         return version;
     }
 
     /**
      * @param version the version to set
      */
     public void setVersion(Integer version) {
         this.version = version;
     }
 
     /**
      * @return the codigo
      */
     public String getCodigo() {
         return codigo;
     }
 
     /**
      * @param codigo the codigo to set
      */
     public void setCodigo(String codigo) {
         this.codigo = codigo;
     }
 
     /**
      * @return the sku
      */
     public String getSku() {
         return sku;
     }
 
     /**
      * @param sku the sku to set
      */
     public void setSku(String sku) {
         this.sku = sku;
     }
 
     /**
      * @return the nombre
      */
     public String getNombre() {
         return nombre;
     }
 
     /**
      * @param nombre the nombre to set
      */
     public void setNombre(String nombre) {
         this.nombre = nombre;
     }
 
     /**
      * @return the descripcion
      */
     public String getDescripcion() {
         return descripcion;
     }
 
     /**
      * @param descripcion the descripcion to set
      */
     public void setDescripcion(String descripcion) {
         this.descripcion = descripcion;
     }
 
     /**
      * @return the marca
      */
     public String getMarca() {
         return marca;
     }
 
     /**
      * @param marca the marca to set
      */
     public void setMarca(String marca) {
         this.marca = marca;
     }
 
     /**
      * @return the modelo
      */
     public String getModelo() {
         return modelo;
     }
 
     /**
      * @param modelo the modelo to set
      */
     public void setModelo(String modelo) {
         this.modelo = modelo;
     }
 
     /**
      * @return the unidadMedida
      */
     public String getUnidadMedida() {
         return unidadMedida;
     }
 
     /**
      * @param unidadMedida the unidadMedida to set
      */
     public void setUnidadMedida(String unidadMedida) {
         this.unidadMedida = unidadMedida;
     }
 
     /**
      * @return the ubicacion
      */
     public String getUbicacion() {
         return ubicacion;
     }
 
     /**
      * @param ubicacion the ubicacion to set
      */
     public void setUbicacion(String ubicacion) {
         this.ubicacion = ubicacion;
     }
 
     /**
      * @return the precioUnitario
      */
     public BigDecimal getPrecioUnitario() {
         return precioUnitario;
     }
 
     /**
      * @param precioUnitario the precioUnitario to set
      */
     public void setPrecioUnitario(BigDecimal precioUnitario) {
         this.precioUnitario = precioUnitario;
     }
 
     /**
      * @return the ultimoPrecio
      */
     public BigDecimal getUltimoPrecio() {
         return ultimoPrecio;
     }
 
     /**
      * @param ultimoPrecio the ultimoPrecio to set
      */
     public void setUltimoPrecio(BigDecimal ultimoPrecio) {
         this.ultimoPrecio = ultimoPrecio;
     }
 
     /**
      * @return the existencia
      */
     public BigDecimal getExistencia() {
         return existencia;
     }
 
     /**
      * @param existencia the existencia to set
      */
     public void setExistencia(BigDecimal existencia) {
         this.existencia = existencia;
     }
 
     /**
      * @return the puntoReorden
      */
     public BigDecimal getPuntoReorden() {
         return puntoReorden;
     }
 
     /**
      * @param puntoReorden the puntoReorden to set
      */
     public void setPuntoReorden(BigDecimal puntoReorden) {
         this.puntoReorden = puntoReorden;
     }
 
     /**
      * @return the iva
      */
     public BigDecimal getIva() {
         return iva;
     }
 
     /**
      * @param iva the iva to set
      */
     public void setIva(BigDecimal iva) {
         this.iva = iva;
     }
 
     /**
      * @return the tiempoEntrega
      */
     public Integer getTiempoEntrega() {
         return tiempoEntrega;
     }
 
     /**
      * @param tiempoEntrega the tiempoEntrega to set
      */
     public void setTiempoEntrega(Integer tiempoEntrega) {
         this.tiempoEntrega = tiempoEntrega;
     }
 
     /**
      * @return the fraccion
      */
     public Boolean getFraccion() {
         return fraccion;
     }
 
     /**
      * @param fraccion the fraccion to set
      */
     public void setFraccion(Boolean fraccion) {
         this.fraccion = fraccion;
     }
 
     /**
      * @return the tipoProducto
      */
     public TipoProducto getTipoProducto() {
         return tipoProducto;
     }
 
     /**
      * @param tipoProducto the tipoProducto to set
      */
     public void setTipoProducto(TipoProducto tipoProducto) {
         this.tipoProducto = tipoProducto;
     }
 
     /**
      * @return the almacen
      */
     public Almacen getAlmacen() {
         return almacen;
     }
 
     /**
      * @param almacen the almacen to set
      */
     public void setAlmacen(Almacen almacen) {
         this.almacen = almacen;
     }
 
     @Override
     public boolean equals(Object obj) {
         if (obj == null) {
             return false;
         }
         if (getClass() != obj.getClass()) {
             return false;
         }
         final Producto other = (Producto) obj;
         if (!Objects.equals(this.sku, other.sku)) {
             return false;
         }
         return true;
     }
 
     @Override
     public int hashCode() {
         int hash = 7;
         hash = 71 * hash + Objects.hashCode(this.id);
         hash = 71 * hash + Objects.hashCode(this.version);
         hash = 71 * hash + Objects.hashCode(this.sku);
         return hash;
     }
 
     @Override
     public String toString() {
         return "Producto{" + "id=" + id + ", sku=" + sku + ", nombre=" + nombre + ", precioUnitario=" + precioUnitario + ", existencia=" + existencia + ", iva=" + iva + ", fraccion=" + fraccion + '}';
     }
 }
