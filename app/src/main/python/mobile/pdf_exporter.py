import os
from datetime import datetime
from fpdf import FPDF
from mobile.attendance import get_attendance_summary, get_calendar_config
from mobile.stats_engine import calculate_attendance_stats

class AttendancePDF(FPDF):
    def header(self):
        self.set_font("helvetica", "B", 15)
        self.set_text_color(31, 78, 120)
        self.cell(0, 10, "INFORME DE ASISTENCIA ACADÉMICA", border=False, align="C")
        self.ln(10)
        self.set_font("helvetica", "I", 10)
        self.set_text_color(128, 128, 128)
        self.cell(0, 5, f"Generado el: {datetime.now().strftime('%d/%m/%Y %H:%M')}", align="R")
        self.ln(10)

    def footer(self):
        self.set_y(-15)
        self.set_font("helvetica", "I", 8)
        self.set_text_color(169, 169, 169)
        self.cell(0, 10, f"Página {self.page_no()}/{{nb}} - NFCAutomation", align="C")

    def draw_bar_chart(self, title, data, x, y, w, h, color_missed=(255, 100, 100), color_total=(100, 150, 255)):
        self.set_xy(x, y)
        self.set_font("helvetica", "B", 11)
        self.set_text_color(50, 50, 50)
        self.cell(w, 10, title, align="L")
        self.ln(12)
        
        start_y = self.get_y()
        max_val = 1
        for item in data.values():
            max_val = max(max_val, item["total"])
        
        bar_height = (h - 20) / len(data)
        chart_x = x + 40 # Margen para etiquetas de texto
        chart_w = w - 50
        
        idx = 0
        for label, vals in data.items():
            curr_y = start_y + (idx * bar_height)
            
            # Etiqueta
            self.set_xy(x, curr_y + (bar_height/4))
            self.set_font("helvetica", "", 8)
            # Acortar etiqueta si es muy larga
            display_label = (label[:15] + "..") if len(label) > 17 else label
            self.cell(35, bar_height/2, display_label, align="R")
            
            # Barra Total (Fondo)
            total_w = (vals["total"] / max_val) * chart_w if max_val > 0 else 0
            self.set_fill_color(*color_total)
            self.rect(chart_x, curr_y, total_w, bar_height * 0.8, "F")
            
            # Barra Faltas (Superpuesta)
            missed_w = (vals["missed"] / max_val) * chart_w if max_val > 0 else 0
            if missed_w > 0:
                self.set_fill_color(*color_missed)
                self.rect(chart_x, curr_y, missed_w, bar_height * 0.8, "F")
            
            # Texto de valores
            self.set_xy(chart_x + total_w + 2, curr_y + (bar_height/4))
            self.set_text_color(80, 80, 80)
            self.cell(20, bar_height/2, f"{vals['missed']}/{vals['total']}", align="L")
            
            idx += 1
        
        self.set_y(start_y + h)

def generate_attendance_pdf(config_dir, cache_dir, period_type="MONTH", semester="q1"):
    try:
        # 1. Obtener datos
        data = get_attendance_summary(config_dir, period_type, semester, "ALL")
        records = data.get("records", [])
        stats = data.get("stats", {})
        
        # 2. Calcular estadísticas para gráficos
        analysis = calculate_attendance_stats(records)
        
        pdf = AttendancePDF()
        pdf.alias_nb_pages()
        pdf.add_page()
        
        # --- SECCIÓN 1: RESUMEN GLOBAL ---
        pdf.set_font("helvetica", "B", 12)
        pdf.cell(0, 10, f"Resumen del Periodo: {period_type} ({semester.upper()})", ln=True)
        
        pdf.set_font("helvetica", "", 10)
        pdf.set_fill_color(240, 240, 240)
        pdf.cell(50, 8, "Total Clases:", fill=True)
        pdf.cell(0, 8, str(stats.get("total_attended", 0) + stats.get("total_missed", 0)), ln=True)
        
        pdf.cell(50, 8, "Asistidas:", fill=True)
        pdf.set_text_color(0, 128, 0)
        pdf.cell(0, 8, str(stats.get("total_attended", 0)), ln=True)
        
        pdf.set_text_color(0, 0, 0)
        pdf.cell(50, 8, "Faltadas:", fill=True)
        pdf.set_text_color(200, 0, 0)
        pdf.cell(0, 8, str(stats.get("total_missed", 0)), ln=True)
        
        pdf.set_text_color(0, 0, 0)
        pdf.cell(50, 8, "% Asistencia:", fill=True)
        pdf.set_font("helvetica", "B", 10)
        pdf.cell(0, 8, f"{stats.get('percentage', 0)}%", ln=True)
        
        pdf.ln(10)
        
        # --- SECCIÓN 2: GRÁFICOS ---
        y_pos = pdf.get_y()
        # Gráfico por Día
        pdf.draw_bar_chart("Faltas por Día de la Semana", analysis["days"], 10, y_pos, 90, 50)
        
        # Gráfico por Intervalo Horario
        pdf.draw_bar_chart("Faltas por Intervalo Horario", analysis["intervals"], 105, y_pos, 90, 50, color_total=(200, 200, 200))
        
        pdf.ln(10)
        y_pos = pdf.get_y()
        # Gráfico por Asignatura (puede ser más largo)
        pdf.draw_bar_chart("Análisis por Asignatura (Faltas/Total)", analysis["subjects"], 10, y_pos, 185, 80)
        
        # --- SECCIÓN 3: TABLA DE DETALLE ---
        pdf.add_page()
        pdf.set_font("helvetica", "B", 12)
        pdf.cell(0, 10, "Detalle de Clases Faltadas", ln=True)
        pdf.ln(5)
        
        # Cabecera tabla
        pdf.set_font("helvetica", "B", 10)
        pdf.set_fill_color(47, 85, 151)
        pdf.set_text_color(255, 255, 255)
        pdf.cell(30, 8, "Fecha", 1, 0, "C", True)
        pdf.cell(30, 8, "Día", 1, 0, "C", True)
        pdf.cell(20, 8, "Hora", 1, 0, "C", True)
        pdf.cell(70, 8, "Asignatura", 1, 0, "C", True)
        pdf.cell(35, 8, "Tipo", 1, 1, "C", True)
        
        pdf.set_text_color(0, 0, 0)
        pdf.set_font("helvetica", "", 9)
        
        fill = False
        for r in records:
            if r.get("status") == "MISSED":
                pdf.set_fill_color(245, 245, 245)
                pdf.cell(30, 7, r["date"], 1, 0, "C", fill)
                pdf.cell(30, 7, r["day"][:3].upper(), 1, 0, "C", fill)
                pdf.cell(20, 7, r["start"], 1, 0, "C", fill)
                pdf.cell(70, 7, r["subject"][:35], 1, 0, "L", fill)
                pdf.cell(35, 7, "TEORÍA" if r.get("type") == "THEORY" else "PRÁCTICA", 1, 1, "C", fill)
                fill = not fill

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"Reporte_Asistencia_{period_type}_{timestamp}.pdf"
        file_path = os.path.join(cache_dir, filename)
        pdf.output(file_path)
        return file_path

    except Exception as e:
        import traceback
        print(traceback.format_exc())
        raise e
