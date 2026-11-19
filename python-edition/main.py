__version__ = "0.1.0"

import ast
import operator
import random

from kivy.app import App
from kivy.animation import Animation
from kivy.clock import Clock
from kivy.core.clipboard import Clipboard
from kivy.core.window import Window
from kivy.graphics import Color, RoundedRectangle, Line
from kivy.metrics import dp, sp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivy.uix.screenmanager import Screen, ScreenManager, SlideTransition
from kivy.uix.scrollview import ScrollView
from kivy.uix.textinput import TextInput
from kivy.uix.widget import Widget


BG = (0.035, 0.043, 0.085, 1)
PANEL = (0.075, 0.09, 0.16, 1)
PANEL_LIGHT = (0.10, 0.12, 0.21, 1)
CYAN = (0.12, 0.88, 0.92, 1)
VIOLET = (0.62, 0.38, 1, 1)
LIME = (0.69, 0.96, 0.39, 1)
WHITE = (0.93, 0.96, 1, 1)
MUTED = (0.56, 0.63, 0.75, 1)
DANGER = (1, 0.39, 0.48, 1)


def make_label(text, size=sp(14), color=WHITE, bold=False, halign="left"):
    label = Label(
        text=text,
        font_size=size,
        color=color,
        bold=bold,
        halign=halign,
        valign="middle",
        size_hint_y=None,
        height=dp(28),
    )
    label.bind(width=lambda obj, width: setattr(obj, "text_size", (width, None)))
    return label


class Panel(BoxLayout):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.padding = dp(15)
        self.spacing = dp(9)
        with self.canvas.before:
            Color(*PANEL)
            self.bg_rect = RoundedRectangle(pos=self.pos, size=self.size, radius=[dp(18)])
            Color(0.22, 0.31, 0.48, 0.45)
            self.border = Line(rounded_rectangle=(self.x, self.y, self.width, self.height, dp(18)), width=0.8)
        self.bind(pos=self._update_canvas, size=self._update_canvas)

    def _update_canvas(self, *_):
        self.bg_rect.pos = self.pos
        self.bg_rect.size = self.size
        self.border.rounded_rectangle = (self.x, self.y, self.width, self.height, dp(18))


class NeonButton(Button):
    def __init__(self, accent=CYAN, **kwargs):
        super().__init__(**kwargs)
        self.background_normal = ""
        self.background_down = ""
        self.background_color = (0, 0, 0, 0)
        self.color = WHITE
        self.bold = True
        self.font_size = sp(13)
        with self.canvas.before:
            Color(accent[0], accent[1], accent[2], 0.16)
            self.bg_rect = RoundedRectangle(pos=self.pos, size=self.size, radius=[dp(13)])
            Color(accent[0], accent[1], accent[2], 0.75)
            self.outline = Line(rounded_rectangle=(self.x, self.y, self.width, self.height, dp(13)), width=1)
        self.bind(pos=self._update_canvas, size=self._update_canvas)

    def _update_canvas(self, *_):
        self.bg_rect.pos = self.pos
        self.bg_rect.size = self.size
        self.outline.rounded_rectangle = (self.x, self.y, self.width, self.height, dp(13))


class ColorPreview(Widget):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.fill = CYAN
        self.bind(pos=self.draw, size=self.draw)
        self.draw()

    def set_fill(self, rgba):
        self.fill = rgba
        self.draw()

    def draw(self, *_):
        self.canvas.clear()
        with self.canvas:
            Color(*self.fill)
            RoundedRectangle(pos=self.pos, size=self.size, radius=[dp(20)])


class BaseScreen(Screen):
    def page(self, title):
        outer = BoxLayout(orientation="vertical", padding=[dp(16), dp(12), dp(16), dp(16)], spacing=dp(12))
        header = BoxLayout(size_hint_y=None, height=dp(52), spacing=dp(10))
        back = NeonButton(text="‹", accent=VIOLET, size_hint_x=None, width=dp(48), font_size=sp(25))
        back.bind(on_release=lambda *_: self.go_home())
        header.add_widget(back)
        header.add_widget(make_label(title, sp(19), WHITE, True))
        outer.add_widget(header)
        return outer

    def go_home(self):
        App.get_running_app().go("home", "right")


class HomeScreen(Screen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        root = BoxLayout(orientation="vertical", padding=[dp(17), dp(18), dp(17), dp(14)], spacing=dp(14))
        scroll = ScrollView(do_scroll_x=False, bar_width=dp(3), scroll_type=["bars", "content"])
        content = BoxLayout(orientation="vertical", size_hint_y=None, spacing=dp(15), padding=[0, 0, 0, dp(12)])
        content.bind(minimum_height=content.setter("height"))

        brand = BoxLayout(size_hint_y=None, height=dp(62), spacing=dp(11))
        logo = Panel(size_hint_x=None, width=dp(58), padding=dp(5))
        logo.add_widget(make_label("NX", sp(21), CYAN, True, "center"))
        brand.add_widget(logo)
        name_col = BoxLayout(orientation="vertical", spacing=dp(0))
        name_col.add_widget(make_label("NETHA X", sp(21), WHITE, True))
        name_col.add_widget(make_label("PYTHON LAB  /  EDITION 01", sp(10), CYAN, True))
        brand.add_widget(name_col)
        content.add_widget(brand)

        hero = Panel(orientation="vertical", size_hint_y=None, height=dp(160), padding=dp(17), spacing=dp(7))
        status_row = BoxLayout(size_hint_y=None, height=dp(25), spacing=dp(7))
        dot = Label(text="●", color=LIME, font_size=sp(13), size_hint_x=None, width=dp(18))
        pulse = Animation(opacity=0.25, duration=0.75) + Animation(opacity=1, duration=0.75)
        pulse.repeat = True
        pulse.start(dot)
        status_row.add_widget(dot)
        status_row.add_widget(make_label("PYTHON CORE  •  READY", sp(11), LIME, True))
        hero.add_widget(status_row)
        hero.add_widget(make_label("A new kind of pocket lab.", sp(22), WHITE, True))
        hero.add_widget(make_label("Small tools. Fast actions. Built with Python.", sp(12), MUTED))
        hero.add_widget(make_label("LOCAL MODE  /  NO ACCOUNT REQUIRED", sp(9), CYAN, True))
        content.add_widget(hero)

        content.add_widget(make_label("YOUR TOOLKIT", sp(12), MUTED, True))
        grid = GridLayout(cols=2, spacing=dp(11), size_hint_y=None, row_default_height=dp(132), row_force_default=True)
        grid.bind(minimum_height=grid.setter("height"))
        modules = [
            ("01", "QUICK CALC", "A clean mini calculator", "calculator", CYAN),
            ("02", "TEXT FORGE", "Transform and count text", "text", VIOLET),
            ("03", "UNIT SHIFT", "Convert useful units", "converter", LIME),
            ("04", "FOCUS FLOW", "A simple focus timer", "focus", CYAN),
            ("05", "COLOR STUDIO", "Create a color palette", "colors", VIOLET),
        ]
        for number, title, subtitle, screen_name, accent in modules:
            card = Panel(orientation="vertical", padding=dp(12), spacing=dp(4))
            card.add_widget(make_label(number, sp(10), accent, True))
            card.add_widget(make_label(title, sp(14), WHITE, True))
            card.add_widget(make_label(subtitle, sp(10), MUTED))
            button = NeonButton(text="OPEN  →", accent=accent, size_hint_y=None, height=dp(34))
            button.bind(on_release=lambda *_args, target=screen_name: App.get_running_app().go(target))
            card.add_widget(button)
            grid.add_widget(card)
        content.add_widget(grid)
        content.add_widget(make_label("NETHA X  •  ALL MODULES RUN LOCALLY", sp(9), MUTED, False, "center"))
        scroll.add_widget(content)
        root.add_widget(scroll)
        self.add_widget(root)


class CalculatorScreen(BaseScreen):
    OPS = {
        ast.Add: operator.add,
        ast.Sub: operator.sub,
        ast.Mult: operator.mul,
        ast.Div: operator.truediv,
        ast.FloorDiv: operator.floordiv,
        ast.Mod: operator.mod,
        ast.Pow: operator.pow,
        ast.UAdd: operator.pos,
        ast.USub: operator.neg,
    }

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        root = self.page("QUICK CALC")
        root.add_widget(make_label("A small calculator with safe arithmetic.", sp(12), MUTED))
        panel = Panel(orientation="vertical", size_hint_y=None, height=dp(260))
        panel.add_widget(make_label("EXPRESSION", sp(10), CYAN, True))
        self.expression = TextInput(
            hint_text="Example: (24 + 8) / 4",
            multiline=False, size_hint_y=None, height=dp(54), font_size=sp(17),
            foreground_color=WHITE, hint_text_color=MUTED, background_normal="",
            background_active="", background_color=PANEL_LIGHT, cursor_color=CYAN,
            padding=[dp(12), dp(14)],
        )
        panel.add_widget(self.expression)
        self.result = make_label("RESULT  —", sp(18), LIME, True)
        panel.add_widget(self.result)
        calculate = NeonButton(text="CALCULATE", accent=CYAN, size_hint_y=None, height=dp(48))
        calculate.bind(on_release=self.calculate)
        panel.add_widget(calculate)
        clear = NeonButton(text="CLEAR", accent=VIOLET, size_hint_y=None, height=dp(42))
        clear.bind(on_release=lambda *_: self.clear())
        panel.add_widget(clear)
        root.add_widget(panel)
        root.add_widget(make_label("Only basic number operations are accepted. No code execution.", sp(10), MUTED))
        self.add_widget(root)

    def clear(self):
        self.expression.text = ""
        self.result.text = "RESULT  —"

    def _calculate_node(self, node):
        if isinstance(node, ast.Expression):
            return self._calculate_node(node.body)
        if isinstance(node, ast.Constant) and type(node.value) in (int, float):
            return node.value
        if isinstance(node, ast.UnaryOp) and type(node.op) in self.OPS:
            return self.OPS[type(node.op)](self._calculate_node(node.operand))
        if isinstance(node, ast.BinOp) and type(node.op) in self.OPS:
            left = self._calculate_node(node.left)
            right = self._calculate_node(node.right)
            if isinstance(node.op, ast.Pow) and (abs(right) > 8 or abs(left) > 1000000):
                raise ValueError("Power is too large")
            value = self.OPS[type(node.op)](left, right)
            if abs(value) > 1e15:
                raise ValueError("Result is too large")
            return value
        raise ValueError("Unsupported expression")

    def calculate(self, *_):
        raw = self.expression.text.strip()
        if not raw or len(raw) > 70:
            self.result.text = "Enter a short expression"
            return
        try:
            answer = self._calculate_node(ast.parse(raw, mode="eval"))
            if isinstance(answer, float):
                answer = round(answer, 8)
            self.result.text = "RESULT  " + str(answer)
        except ZeroDivisionError:
            self.result.text = "Cannot divide by zero"
        except Exception:
            self.result.text = "Check the expression"


class TextForgeScreen(BaseScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        root = self.page("TEXT FORGE")
        root.add_widget(make_label("Transform text and check its size.", sp(12), MUTED))
        self.editor = TextInput(
            hint_text="Type or paste your text here...", multiline=True, size_hint_y=1,
            font_size=sp(15), foreground_color=WHITE, hint_text_color=MUTED,
            background_normal="", background_active="", background_color=PANEL_LIGHT,
            cursor_color=CYAN, padding=dp(12),
        )
        root.add_widget(self.editor)
        self.stats = make_label("0 words   •   0 characters", sp(11), LIME, True)
        root.add_widget(self.stats)
        count_btn = NeonButton(text="UPDATE COUNT", accent=CYAN, size_hint_y=None, height=dp(42))
        count_btn.bind(on_release=self.update_count)
        root.add_widget(count_btn)
        row = BoxLayout(size_hint_y=None, height=dp(43), spacing=dp(7))
        for title, action, accent in [
            ("UPPERCASE", str.upper, VIOLET),
            ("lowercase", str.lower, CYAN),
            ("Title Case", str.title, LIME),
        ]:
            btn = NeonButton(text=title, accent=accent)
            btn.bind(on_release=lambda *_args, fn=action: self.transform(fn))
            row.add_widget(btn)
        root.add_widget(row)
        clear = NeonButton(text="CLEAR TEXT", accent=DANGER, size_hint_y=None, height=dp(40))
        clear.bind(on_release=lambda *_: self.clear_text())
        root.add_widget(clear)
        self.add_widget(root)

    def update_count(self, *_):
        value = self.editor.text
        self.stats.text = "{} words   •   {} characters".format(len(value.split()), len(value))

    def transform(self, fn):
        self.editor.text = fn(self.editor.text)
        self.update_count()

    def clear_text(self):
        self.editor.text = ""
        self.update_count()


class ConverterScreen(BaseScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        root = self.page("UNIT SHIFT")
        root.add_widget(make_label("Everyday conversions, without the clutter.", sp(12), MUTED))

        km_panel = Panel(orientation="vertical", size_hint_y=None, height=dp(205))
        km_panel.add_widget(make_label("DISTANCE  /  KM  →  MILES", sp(11), CYAN, True))
        self.km_input = TextInput(
            hint_text="Enter kilometres", multiline=False, size_hint_y=None, height=dp(48),
            font_size=sp(16), foreground_color=WHITE, hint_text_color=MUTED,
            background_normal="", background_active="", background_color=PANEL_LIGHT,
            cursor_color=CYAN, padding=dp(12),
        )
        km_panel.add_widget(self.km_input)
        km_btn = NeonButton(text="CONVERT TO MILES", accent=CYAN, size_hint_y=None, height=dp(42))
        km_btn.bind(on_release=self.km_to_miles)
        km_panel.add_widget(km_btn)
        self.km_result = make_label("RESULT  —", sp(15), LIME, True)
        km_panel.add_widget(self.km_result)
        root.add_widget(km_panel)

        temp_panel = Panel(orientation="vertical", size_hint_y=None, height=dp(205))
        temp_panel.add_widget(make_label("TEMPERATURE  /  °C  →  °F", sp(11), VIOLET, True))
        self.temp_input = TextInput(
            hint_text="Enter Celsius", multiline=False, size_hint_y=None, height=dp(48),
            font_size=sp(16), foreground_color=WHITE, hint_text_color=MUTED,
            background_normal="", background_active="", background_color=PANEL_LIGHT,
            cursor_color=VIOLET, padding=dp(12),
        )
        temp_panel.add_widget(self.temp_input)
        temp_btn = NeonButton(text="CONVERT TO FAHRENHEIT", accent=VIOLET, size_hint_y=None, height=dp(42))
        temp_btn.bind(on_release=self.celsius_to_f)
        temp_panel.add_widget(temp_btn)
        self.temp_result = make_label("RESULT  —", sp(15), LIME, True)
        temp_panel.add_widget(self.temp_result)
        root.add_widget(temp_panel)
        self.add_widget(root)

    @staticmethod
    def read_number(value):
        try:
            number = float(value.strip())
            if not (-1e9 < number < 1e9):
                return None
            return number
        except (ValueError, AttributeError):
            return None

    def km_to_miles(self, *_):
        value = self.read_number(self.km_input.text)
        self.km_result.text = "Enter a valid number" if value is None else "RESULT  {:.4f} mi".format(value * 0.621371)

    def celsius_to_f(self, *_):
        value = self.read_number(self.temp_input.text)
        self.temp_result.text = "Enter a valid number" if value is None else "RESULT  {:.2f} °F".format((value * 9 / 5) + 32)


class FocusScreen(BaseScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.remaining = 25 * 60
        self.event = None
        self.running = False
        root = self.page("FOCUS FLOW")
        root.add_widget(make_label("One task. One session. Keep your pace.", sp(12), MUTED))
        panel = Panel(orientation="vertical", size_hint_y=None, height=dp(340), padding=dp(18))
        panel.add_widget(make_label("FOCUS SESSION", sp(11), CYAN, True, "center"))
        self.time_label = Label(text="25:00", font_size=sp(48), bold=True, color=WHITE, size_hint_y=1)
        panel.add_widget(self.time_label)
        self.state_label = make_label("READY WHEN YOU ARE", sp(10), MUTED, True, "center")
        panel.add_widget(self.state_label)
        self.start_button = NeonButton(text="START SESSION", accent=LIME, size_hint_y=None, height=dp(50))
        self.start_button.bind(on_release=self.toggle)
        panel.add_widget(self.start_button)
        reset = NeonButton(text="RESET 25:00", accent=VIOLET, size_hint_y=None, height=dp(44))
        reset.bind(on_release=self.reset)
        panel.add_widget(reset)
        root.add_widget(panel)
        root.add_widget(make_label("Tip: take a short break after your focus session.", sp(10), MUTED))
        self.add_widget(root)

    def render_time(self):
        minutes, seconds = divmod(self.remaining, 60)
        self.time_label.text = "{:02d}:{:02d}".format(minutes, seconds)

    def toggle(self, *_):
        if self.running:
            self.pause()
        else:
            self.running = True
            self.start_button.text = "PAUSE SESSION"
            self.state_label.text = "FOCUS MODE  /  RUNNING"
            self.state_label.color = LIME
            self.event = Clock.schedule_interval(self.tick, 1)

    def tick(self, _dt):
        if self.remaining > 0:
            self.remaining -= 1
            self.render_time()
        if self.remaining <= 0:
            self.pause()
            self.state_label.text = "SESSION COMPLETE"
            self.state_label.color = LIME
            self.start_button.text = "START AGAIN"
            return False
        return True

    def pause(self):
        self.running = False
        if self.event is not None:
            self.event.cancel()
            self.event = None
        self.start_button.text = "RESUME SESSION"
        self.state_label.text = "PAUSED"

    def reset(self, *_):
        self.pause()
        self.remaining = 25 * 60
        self.render_time()
        self.start_button.text = "START SESSION"
        self.state_label.text = "READY WHEN YOU ARE"
        self.state_label.color = MUTED


class ColorStudioScreen(BaseScreen):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.current_hex = "#1FE0EA"
        root = self.page("COLOR STUDIO")
        root.add_widget(make_label("Generate a fresh color and copy its HEX value.", sp(12), MUTED))
        panel = Panel(orientation="vertical", size_hint_y=None, height=dp(310))
        self.preview = ColorPreview(size_hint_y=1)
        panel.add_widget(self.preview)
        self.hex_label = make_label(self.current_hex, sp(24), WHITE, True, "center")
        panel.add_widget(self.hex_label)
        generate = NeonButton(text="GENERATE COLOR", accent=VIOLET, size_hint_y=None, height=dp(48))
        generate.bind(on_release=self.generate)
        panel.add_widget(generate)
        copy = NeonButton(text="COPY HEX", accent=CYAN, size_hint_y=None, height=dp(44))
        copy.bind(on_release=self.copy_hex)
        panel.add_widget(copy)
        self.message = make_label("READY TO CREATE", sp(10), MUTED, True, "center")
        root.add_widget(panel)
        root.add_widget(self.message)
        self.add_widget(root)
        self.apply_hex(self.current_hex)

    def apply_hex(self, value):
        self.current_hex = value
        rgb = tuple(int(value[i:i + 2], 16) / 255 for i in (1, 3, 5))
        self.preview.set_fill((*rgb, 1))
        self.hex_label.text = value

    def generate(self, *_):
        value = "#{:02X}{:02X}{:02X}".format(
            random.randint(40, 235),
            random.randint(40, 235),
            random.randint(40, 235),
        )
        self.apply_hex(value)
        self.message.text = "NEW COLOR GENERATED"

    def copy_hex(self, *_):
        Clipboard.copy(self.current_hex)
        self.message.text = "HEX VALUE COPIED"


class NethaXApp(App):
    def build(self):
        self.title = "NETHA X PYTHON LAB"
        Window.clearcolor = BG
        self.manager = ScreenManager(transition=SlideTransition(duration=0.18))
        self.manager.add_widget(HomeScreen(name="home"))
        self.manager.add_widget(CalculatorScreen(name="calculator"))
        self.manager.add_widget(TextForgeScreen(name="text"))
        self.manager.add_widget(ConverterScreen(name="converter"))
        self.manager.add_widget(FocusScreen(name="focus"))
        self.manager.add_widget(ColorStudioScreen(name="colors"))
        return self.manager

    def go(self, screen_name, direction="left"):
        if self.manager.current == screen_name:
            return
        self.manager.transition.direction = direction
        self.manager.current = screen_name


if __name__ == "__main__":
    NethaXApp().run()
